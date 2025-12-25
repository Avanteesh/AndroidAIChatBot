package org.example.project

import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.json.Json
import org.jetbrains.compose.ui.tooling.preview.Preview
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.IOException
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import kotlinx.serialization.encodeToString
import org.example.project.BuildConfig

@Serializable
data class ChatResponse(
    val id: String,
    val choices: List<Choices>
)

@Serializable
data class Choices(
    val index: Int,
    val message: Message
)

@Serializable
data class Message(
    var role: String,
    var content: String
)

@Composable
fun App() {
    MaterialTheme {
        var chatHistory by remember { mutableStateOf(
            listOf(Message(role="user", content=""))
        )}
        val apikey: String = BuildConfig.groqkey
        val json = Json { ignoreUnknownKeys = true }
        var fetchTrigger by remember { mutableStateOf(false) }
        var airesponse: String by remember { mutableStateOf("") }
        var prompt: String by remember { mutableStateOf("") }
        LaunchedEffect(fetchTrigger) {
            if (chatHistory[chatHistory.size - 1].role == "assistant")  {
                chatHistory = chatHistory + listOf(Message(role="user", content=prompt))
            } else if (chatHistory[chatHistory.size - 1].role == "user")  {
                chatHistory[chatHistory.size - 1].content = prompt
            }
            val serializedchat = json.encodeToString<List<Message>>(chatHistory)
            val jsonprompt = """{
              "model": "openai/gpt-oss-120b",
              "messages": $serializedchat
            }""".trimIndent().toRequestBody("application/json".toMediaType())
            val client = OkHttpClient()
            val request = Request.Builder().url("https://api.groq.com/openai/v1/chat/completions").post(jsonprompt)
                .addHeader("Authorization", "Bearer $apikey").addHeader("Content-Type", "application/json")
                .build()
            Log.d(request.headers.toString(), "REQUEST HEADERS DEBUG")
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException)  {
                    Log.d("Error occurred", "ERROR")
                }
                override fun onResponse(call: Call, response: Response) {
                    Log.d("Response code ${response.code}", "DEBUG response")
                    if (response.code == 200)  {
                        val jsontring = response.body.string()
                        val jsondecoded = json.decodeFromString<ChatResponse>(jsontring)
                        chatHistory = (chatHistory + jsondecoded.choices.firstOrNull()?.message) as List<Message>
                        airesponse = chatHistory[chatHistory.size - 1].content
                    } else {
                        Log.d("Response Error ${response.code}", "DEBUG STATUS")
                    }
                }
            })
        }
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(10.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            OutlinedTextField(
                value = prompt,
                onValueChange = { prompt = it },
                placeholder = {Text("Ask something...")},
                modifier = Modifier.fillMaxWidth(0.6f).height(85.dp)
            )
            Button(onClick = { fetchTrigger = !fetchTrigger }) {
                Text("ask")
            }
            Text("AI response: $airesponse")
        }
    }
}