package org.example.project

import android.R
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowCircleUp
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
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
        var fetchTrigger by remember { mutableStateOf(0) }
        var airesponse: String by remember { mutableStateOf("") }
        var prompt: String by remember { mutableStateOf("") }
        var textFocussed by remember { mutableStateOf(false) }
        val scrollState = rememberScrollState()
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
                    Log.d("Error occurred: ${e.message}", "ERROR")
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
                        Log.d("Response Headers: ${response.headers}", "DEBUG ISSUE")
                    }
                }
            })
        }
        Column(
            modifier = Modifier
                .padding(10.dp)
                .fillMaxWidth(1.0f)
                .fillMaxHeight(1.0f)
                .background(
                    brush = Brush.radialGradient(
                        listOf(
                            Color(15, 15, 15), Color(20, 20, 20),
                            Color(25, 25, 25), Color(30, 30, 30),
                            Color(32, 32, 32)
                        )
                    )
                ), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
        ) {
            Box(modifier = Modifier
                .fillMaxWidth(0.99f)
                .fillMaxHeight(0.87f)
                .verticalScroll(state = scrollState)
                .padding(10.dp))  {
                //Text("AI response: $airesponse", color=Color.White)
                Column(Modifier
                    .fillMaxHeight(1.0f)
                    .fillMaxWidth(1.0f)) {
                    chatHistory.forEach { item ->
                        when (item.role)  {
                            "user" -> Row(modifier = Modifier.fillMaxWidth(0.99f).padding(5.dp)) {
                                Spacer(Modifier.fillMaxHeight(0.98f).fillMaxWidth(0.1f))
                                Text(item.content, style = TextStyle(Color.White, fontSize = 12.sp,fontWeight = FontWeight.ExtraBold),
                                    modifier = Modifier.background(
                                        brush = Brush.linearGradient(
                                            listOf(
                                                Color(131, 58, 180),
                                                Color(253, 29, 29),
                                                Color(252, 176, 69)
                                            )
                                        )
                                    ).padding(8.dp))
                            }
                           "assistant" ->  Row(modifier = Modifier.fillMaxWidth(0.99f).padding(5.dp))  {
                               Text(item.content, style = TextStyle(Color.White, fontSize =12.sp, fontWeight = FontWeight.ExtraBold),
                                   modifier = Modifier.background(
                                       brush = Brush.linearGradient(
                                           listOf(Color(42, 123, 155),
                                           Color(87, 199, 133),
                                           Color(237, 221, 83))
                                       )
                                   ).padding(8.dp),fontSize = 12.sp)
                               Spacer(Modifier.fillMaxHeight(0.98f).fillMaxWidth(0.1f))
                           }
                        }
                    }
                }
            }
            Row(modifier = Modifier.fillMaxWidth(0.9f), horizontalArrangement = Arrangement.Center) {
                OutlinedTextField(
                    value = prompt,
                    onValueChange = {
                        prompt = it
                        textFocussed = if (!prompt.isBlank()) true else false
                                    },
                    placeholder = { Text("Ask something...", color = Color.White) },
                    modifier = Modifier
                        .fillMaxWidth(0.99f)
                        .height(95.dp)
                        .padding(12.dp),
                    textStyle = TextStyle(color = Color.White),
                    colors = OutlinedTextFieldDefaults.colors(
                       focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color(180,180,180),
                        focusedTrailingIconColor = Color.White
                    ),
                    trailingIcon = {
                        Button(
                            onClick = {
                                if (textFocussed) { fetchTrigger++ }
                            },
                            colors = ButtonDefaults.buttonColors(Color.Transparent),
                            modifier = Modifier.size(45.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowCircleUp,
                                contentDescription = null,
                                modifier = Modifier.size(52.dp).padding(5.dp),
                                tint = if (textFocussed) Color.White else Color(160, 160, 160)
                            )
                        }
                    },shape = RoundedCornerShape(30.dp)
                )
            }
        }
    }
}