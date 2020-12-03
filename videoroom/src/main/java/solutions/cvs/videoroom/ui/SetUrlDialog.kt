package solutions.cvs.videoroom.ui

import android.app.Dialog
import android.content.Context
import solutions.cvs.videoroom.R
import android.content.DialogInterface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import solutions.cvs.videoroom.MainActivity
import solutions.cvs.videoroom.databinding.SetUrlBinding


class SetUrlDialog : DialogFragment(), DialogInterface.OnClickListener {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = activity?.let { activity ->
            val builder = AlertDialog.Builder(activity)
            val inflater = requireActivity().layoutInflater;
            val sPref = activity.applicationContext.getSharedPreferences("videoroom", Context.MODE_PRIVATE)
            val dataBinding = DataBindingUtil.inflate<SetUrlBinding>(inflater, R.layout.dialog_set_url, null, false).also {
                it.editUrl.setText(sPref.getString("baseUrl", "https://cvs.solutions"))
            }
            val view = dataBinding.root

            builder.setView(view)
                // Add action buttons
                .setPositiveButton(R.string.save,
                    DialogInterface.OnClickListener { dialog, id ->
                        val baseUrl = dataBinding.editUrl.text.toString()
                        sPref.edit().run {
                            putString("baseUrl", baseUrl)
                            apply()
                            context?.let { context ->  (activity as MainActivity).triggerRestart(context) }
                        }
                    })
                .setNegativeButton(R.string.cancel,
                    DialogInterface.OnClickListener { dialog, id ->
                        dialog?.cancel()
                    })
            builder.setTitle("Server URL")
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
        return dialog
    }


    override fun onClick(dialog: DialogInterface, which: Int) {
        Log.d("DIALOG", "${which}")
    }
}