package dreadloaf.com.htn2018.select

import android.content.Context
import dreadloaf.com.htn2018.Mole

class SelectPresenter(val mView : SelectView, val mInteractor: SelectInteractor) : SelectInteractor.OnCompleteLoadListener{
    override fun onComplete(moles: List<Mole>) {
        mView.populateRecylcerView(moles)
    }

    fun loadSavedMoles(context : Context){
        mInteractor.loadSavedMoles(this)
    }
}