package dreadloaf.com.htn2018.interact

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import java.text.SimpleDateFormat
import java.util.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import android.util.Log
import dreadloaf.com.htn2018.Client
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.io.*


class InteractInteractor {

    interface OnUploadCompleteListener{
        fun onSuccess()
        fun onFailure()
    }

    private lateinit var mStorageRef : StorageReference
    private lateinit var mCurrentPhotoPath :String

    @Throws(IOException::class)
    fun createImageFile(context: Context): File {
        // Create an image file name
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_"
        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val image = File.createTempFile(
                imageFileName, /* prefix */
                ".jpg", /* suffix */
                storageDir      /* directory */
        )

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.absolutePath
        return image
    }



    fun encodeImageAndUpload(bitmap: Bitmap){
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
        val encodedImage = Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray())


    }

    fun uploadImageToStorage(imageUri: Uri, listener: OnUploadCompleteListener){
        mStorageRef = FirebaseStorage.getInstance().reference
        val filePath = mStorageRef.child(imageUri.lastPathSegment)

        filePath.putFile(imageUri).addOnSuccessListener({ taskSnapshot ->
            val downloadUrl = taskSnapshot.downloadUrl
            listener.onSuccess()

        })
        filePath.putFile(imageUri).addOnFailureListener({taskSnapshot ->
            listener.onFailure()
        })
    }

    fun analyzeImage(photoUri: Uri, context: Context){
        val builder : Retrofit.Builder = Retrofit.Builder()
                .baseUrl("https://southcentralus.api.cognitive.microsoft.com/customvision/v2.0/Prediction/")
                .addConverterFactory(ScalarsConverterFactory.create())




        val retrofit : Retrofit = builder.build()
        val client : Client = retrofit.create(Client::class.java)
        val byteArray = uriToBytes(photoUri, context)



        val call : Call<ResponseBody> = client.analyzeImage(RequestBody.create(MediaType.parse("application/octet-stream"), byteArray), "4765c058-f675-4d87-a70a-4ac6501396f0")
        call.enqueue(object: Callback<ResponseBody>{
            override fun onFailure(call: Call<ResponseBody>?, t: Throwable?) {
                Log.e("InteractInteractor", t.toString())
            }

            override fun onResponse(call: Call<ResponseBody>?, response: Response<ResponseBody>?) {
                val jsonString = response?.body()?.string()
                val jsonObject = JSONObject(jsonString)
                val array: JSONArray = jsonObject.getJSONArray("predictions")
                val predictionValues = array.getJSONObject(0)

                val probability :Double = predictionValues["probability"] as Double
                val type : String= predictionValues["tagName"] as String
                

            }
        })

    }



    fun uriToBytes(uri: Uri, context: Context) : ByteArray{
        try{
            val inputStream : InputStream = context.contentResolver.openInputStream(uri)
            val byteBuffer : ByteArrayOutputStream = ByteArrayOutputStream()
            val bufferSize : Int = 1024
            val buffer = ByteArray(bufferSize)
            var len = 0
            do{

                len = inputStream.read(buffer)
                if(len == -1){
                    break
                }
                byteBuffer.write(buffer, 0, len)
            } while ( len != -1)
            return byteBuffer.toByteArray()
        }catch (e : FileNotFoundException){
            e.printStackTrace()
        }
        return ByteArray(0)
    }


}