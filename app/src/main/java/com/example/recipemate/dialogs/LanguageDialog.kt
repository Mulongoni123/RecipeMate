package com.example.recipemate.dialogs

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.recipemate.R
import com.example.recipemate.utils.LanguageManager

class LanguageDialog : DialogFragment() {

    private lateinit var languageManager: LanguageManager
    private var onLanguageChanged: (() -> Unit)? = null

    companion object {
        fun newInstance(onLanguageChanged: () -> Unit): LanguageDialog {
            return LanguageDialog().apply {
                this.onLanguageChanged = onLanguageChanged
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        languageManager = LanguageManager(requireContext())

        val inflater = LayoutInflater.from(requireContext())
        val view = inflater.inflate(R.layout.dialog_language, null)

        val radioGroup = view.findViewById<RadioGroup>(R.id.radioGroupLanguages)
        val rbEnglish = view.findViewById<RadioButton>(R.id.rbEnglish)
        val rbAfrikaans = view.findViewById<RadioButton>(R.id.rbAfrikaans)

        // Set current selection
        when (languageManager.getCurrentLanguage()) {
            LanguageManager.LANGUAGE_ENGLISH -> rbEnglish.isChecked = true
            LanguageManager.LANGUAGE_AFRIKAANS -> rbAfrikaans.isChecked = true
        }

        return AlertDialog.Builder(requireContext())
            .setView(view)
            .setTitle(R.string.language_setting)
            .setPositiveButton(R.string.save) { dialog, _ ->
                val selectedId = radioGroup.checkedRadioButtonId
                when (selectedId) {
                    R.id.rbEnglish -> {
                        languageManager.setLanguage(LanguageManager.LANGUAGE_ENGLISH)
                        Log.d("LanguageDialog", "Language changed to English")
                    }
                    R.id.rbAfrikaans -> {
                        languageManager.setLanguage(LanguageManager.LANGUAGE_AFRIKAANS)
                        Log.d("LanguageDialog", "Language changed to Afrikaans")
                    }
                }
                onLanguageChanged?.invoke()
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .create()
    }
}