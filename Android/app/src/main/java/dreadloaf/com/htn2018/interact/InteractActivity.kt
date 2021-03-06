package dreadloaf.com.htn2018.interact

import android.app.PendingIntent.getActivity
import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import android.support.v7.app.AppCompatActivity
import dreadloaf.com.htn2018.R
import android.provider.MediaStore
import android.graphics.Bitmap
import android.net.Uri
import android.support.v4.content.FileProvider
import android.R.attr.data
import android.support.v4.app.NotificationCompat.getExtras
import android.util.Log
import dreadloaf.com.htn2018.DateUtils
import dreadloaf.com.htn2018.Mole
import android.content.Context.MODE_PRIVATE
import android.R.id.edit
import android.content.Context
import android.content.SharedPreferences
import android.R.id.edit
import android.graphics.BitmapFactory
import android.opengl.Visibility
import android.view.View
import android.widget.*
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.math.roundToInt


class InteractActivity  : AppCompatActivity(), InteractView {

    lateinit var mPresenter : InteractPresenter
    val REQUEST_IMAGE_CAPTURE = 1
    lateinit var photoURI : Uri


    var mTracking : Boolean = false
    var mId : Long = -1
    lateinit var mTrackingButton : Button
    lateinit var mProgressBar : ProgressBar
    lateinit var mTakePhotoButton :Button
    lateinit var mImagePath :String
    lateinit var mRiskPercentText : TextView
    lateinit var mRiskValueText : TextView
    lateinit var mIdText : TextView
    lateinit var mImageView : ImageView
    lateinit var mDateText : TextView
    lateinit var mNote : TextView

    override  fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_interact)

        mTrackingButton = findViewById(R.id.tracking_button)
        mProgressBar = findViewById(R.id.analysis_loading)
        mImageView = findViewById(R.id.mole_image)
        mDateText = findViewById(R.id.date_logged_interact_view)
        mIdText = findViewById(R.id.mole_id_interact_view)
        mRiskPercentText = findViewById(R.id.malignant_percent)
        mRiskValueText = findViewById(R.id.risk_text)
        mTakePhotoButton = findViewById(R.id.take_photo_button)
        mNote = findViewById(R.id.note_text)


        val prevIntent = intent
        //Then it came from the onClick
        if(prevIntent.hasExtra("id")){
            val id = prevIntent.extras.get("id") as Long
            val date = prevIntent.extras.get("date")
            val riskPercent : Double = prevIntent.extras.get("riskPercent") as Double
            val riskValue : String = prevIntent.extras.get("riskValue").toString()
            val imageDir = prevIntent.extras.get("imageDir")
            val tracking = prevIntent.extras.get("tracking") as Boolean
            mTracking = tracking
            mId = id
            if(tracking){
                mTrackingButton.text = "Stop Tracking"
            }else{
                mTrackingButton.text = "Begin Tracking"
            }

            val database = FirebaseDatabase.getInstance().reference
            database.child(mId.toString()).addValueEventListener(object: ValueEventListener{
                override fun onCancelled(p0: DatabaseError?) {
                    Log.e("idk", p0.toString())
                }

                override fun onDataChange(p0: DataSnapshot?) {
                    val data = p0?.value.toString()
                    if(data.indexOf("=") != -1 && data.indexOf("}") != -1){
                        val str = data.substring(data.indexOf("=")+1, data.indexOf("}"))
                        mNote.text = str
                    }

                }
            })

            Log.e("InteractActivity", "Happening")
            mIdText.text = id.toString()
            //TODO: If want to display id remove thsis line
            mIdText.visibility = View.GONE
            mDateText.text = date.toString()
            var formattedRisk : Int
            if(riskPercent < 1){
                 formattedRisk = (riskPercent * 100).roundToInt()
                if(riskValue == "Low"){
                    formattedRisk = 100 - formattedRisk
                }
            }else{
                formattedRisk = riskPercent.toInt()
            }
            //Edge case
            if(riskValue == "Low" && formattedRisk == 100){
                formattedRisk = 0
            }
            mRiskPercentText.text = formattedRisk.toString() + "%"
            mRiskValueText.text = riskValue + " Risk: "
            val bitmap = BitmapFactory.decodeFile(imageDir.toString())
            mImageView.setImageBitmap(bitmap)
            mTakePhotoButton.visibility = View.GONE
        }else{
            mRiskPercentText.visibility = View.GONE
            mRiskValueText.text = "Pending..."
        }


        mPresenter = InteractPresenter(this, InteractInteractor())

        mDateText.text = DateUtils.getTodayDateFormatted()


        mTrackingButton.setOnClickListener({
            mTracking = !mTracking

            if(mTracking){
                mTrackingButton.text = "Stop Tracking"
            }else{
                mTrackingButton.text = "Begin Tracking"
            }

        })

        mTakePhotoButton.setOnClickListener({
            val takePictureIntent : Intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if(takePictureIntent.resolveActivity(packageManager) != null) {

                val photoFile = mPresenter.createFile(this)
                mImagePath = photoFile.absolutePath
                photoURI = FileProvider.getUriForFile(this,
                        "com.example.android.fileprovider",
                        photoFile)

                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                Log.e("InteractActivity", "started activity")
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
                mProgressBar.visibility = View.VISIBLE
            }


        })



    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        //super.onActivityResult(requestCode, resultCode, data)
        Log.e("InteractActivity", "on activity result")
        if(requestCode == 1 && resultCode == RESULT_OK){
            val extras = data?.extras

            val bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, photoURI)
            mImageView.setImageBitmap(bitmap)

            mPresenter.uploadImageToStorage(photoURI)
        }
    }

    override fun onSuccessfulUpload() {
        Toast.makeText(this, "Successfuly uploaded file", Toast.LENGTH_LONG).show()
        mPresenter.analyze(photoURI, this)

    }
    override fun onFailedUpload() {
        Toast.makeText(this, "Failed to upload file", Toast.LENGTH_LONG).show()
    }

    override fun onSuccessfulAnalysis(probability: Double, type: String) {
        var risk = ""
        var formattedPercent = (probability*100).roundToInt()
        if(type == "Malignant"){
            risk = "High"
        }else{
            risk = "Low"
            formattedPercent = 100 - formattedPercent
        }

        mRiskValueText.text = risk + " Risk: "
        mRiskPercentText.text = formattedPercent.toString() + "%"
        mRiskPercentText.visibility = View.VISIBLE
        mTakePhotoButton.visibility = View.GONE
        mProgressBar.visibility = View.GONE
        mId = getNextId().toLong()


        val mole = Mole(mId, mDateText.text.toString(), probability, risk, mImagePath, mTracking )
        Log.e("InteractActivity", "saving moles")
        mPresenter.saveMoles(mole)

    }

    override fun onSuccessfulSave() {
        Toast.makeText(this, "Successfully Saved Moles", Toast.LENGTH_LONG).show()
        val database = FirebaseDatabase.getInstance().reference
        database.child(mId.toString()).addValueEventListener(object: ValueEventListener{
            override fun onCancelled(p0: DatabaseError?) {
                Log.e("idk", p0.toString())
            }

            override fun onDataChange(p0: DataSnapshot?) {
                val data = p0?.value.toString()
                if(data.indexOf("=") != -1 && data.indexOf("}") != -1){
                    val str = data.substring(data.indexOf("=")+1, data.indexOf("}"))
                    mNote.text = str
                }

            }
        })

    }

    fun getNextId(): Int{
        val sharedPref = getSharedPreferences("appData", Context.MODE_PRIVATE)
        val prefEditor = getSharedPreferences("appData", Context.MODE_PRIVATE).edit()

        val id = sharedPref.getInt("id", 1)

        prefEditor.putInt("id", id+1)
        prefEditor.apply()
        return id

    }
}