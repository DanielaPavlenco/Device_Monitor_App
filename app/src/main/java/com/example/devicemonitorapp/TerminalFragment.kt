package com.example.devicemonitorapp

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.Fragment
import java.io.BufferedReader
import java.io.InputStreamReader

class TerminalFragment : Fragment() {

    private lateinit var terminalOutput: TextView
    private lateinit var commandInput: EditText
    private lateinit var scrollView: ScrollView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_terminal, container, false)

        terminalOutput = view.findViewById(R.id.terminal_output)
        commandInput = view.findViewById(R.id.command_input)
        scrollView = view.findViewById(R.id.scroll_view)

        commandInput.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE || event?.keyCode == KeyEvent.KEYCODE_ENTER) {
                executeCommand()
                true
            } else {
                false
            }
        }

        commandInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (s.toString().endsWith("\n")) {
                    s?.replace(s.length - 1, s.length, "")
                    executeCommand()
                }
            }
        })

        return view
    }

    private fun executeCommand() {
        val command = commandInput.text.toString()
        if (command.isNotEmpty()) {
            try {
                val process = Runtime.getRuntime().exec(command)
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                val errorReader = BufferedReader(InputStreamReader(process.errorStream))
                val output = StringBuilder()
                val errorOutput = StringBuilder()
                var line: String?

                while (reader.readLine().also { line = it } != null) {
                    output.append(line).append("\n")
                }

                while (errorReader.readLine().also { line = it } != null) {
                    errorOutput.append(line).append("\n")
                }

                // Prepend the command and output (including errors) to the terminal output
                val newOutput = "> $command\n$output$errorOutput"
                terminalOutput.text = newOutput + terminalOutput.text.toString()

                // Scroll
                scrollView.post {
                    scrollView.fullScroll(View.FOCUS_UP)
                }

                // Clear la comanda de input
                commandInput.setText("")

                process.waitFor()
            } catch (e: Exception) {
                val newOutput = "> $command\n${e.message}\n"
                terminalOutput.text = newOutput + terminalOutput.text.toString()
            }
        }
    }
}
