package com.example.root

import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.DataOutputStream
import java.io.File

data class CommandResult(
    val command: String,
    val isSuccess: Boolean,
    val output: String,
    val error: String,
    val exitCode: Int
)

object RootCommandExecutor {
    private const val TAG = "RootCommandExecutor"

    /**
     * Checks if the device has the superuser (su) binary available in system paths.
     */
    fun isRootAvailable(): Boolean {
        val paths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su"
        )
        for (path in paths) {
            if (File(path).exists()) return true
        }
        return checkSuBinaryWithWhich()
    }

    private fun checkSuBinaryWithWhich(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("which", "su"))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val line = reader.readLine()
            process.destroy()
            line != null && line.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Executes a single command as root (su).
     */
    fun executeRootCommand(command: String): CommandResult {
        Log.d(TAG, "Executing root command: $command")
        var process: Process? = null
        var os: DataOutputStream? = null
        var isSuccess = false
        var exitCode = -1
        val stdout = StringBuilder()
        val stderr = StringBuilder()

        try {
            process = Runtime.getRuntime().exec("su")
            os = DataOutputStream(process.outputStream)
            
            // Stream the commands to shell
            os.write(("$command\n").toByteArray(Charsets.UTF_8))
            os.write("exit\n".toByteArray(Charsets.UTF_8))
            os.flush()

            val stdoutReader = BufferedReader(InputStreamReader(process.inputStream))
            val stderrReader = BufferedReader(InputStreamReader(process.errorStream))

            var oLine: String?
            while (stdoutReader.readLine().also { oLine = it } != null) {
                stdout.append(oLine).append("\n")
            }
            
            var eLine: String?
            while (stderrReader.readLine().also { eLine = it } != null) {
                stderr.append(eLine).append("\n")
            }

            exitCode = process.waitFor()
            isSuccess = (exitCode == 0)
        } catch (e: Exception) {
            Log.e(TAG, "Error executing root command", e)
            stderr.append("Exception: ").append(e.message)
        } finally {
            try {
                os?.close()
                process?.destroy()
            } catch (e: Exception) {
                // Ignore
            }
        }

        return CommandResult(
            command = command,
            isSuccess = isSuccess,
            output = stdout.toString().trim(),
            error = stderr.toString().trim(),
            exitCode = exitCode
        )
    }
}
