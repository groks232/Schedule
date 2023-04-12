package com.example.schedule

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.schedule.parsing.algorithm
import com.example.schedule.ui.theme.ScheduleTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.File

class ScheduleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ScheduleTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    Column {
                        val groupName = intent.getStringExtra("groupName")
                        val groupNum = intent.getStringExtra("groupNumber")
                        InterfaceDraw(this@ScheduleActivity, "$groupName $groupNum")
                    }
                }
            }
        }
    }
}

@Composable
fun LoadingScreen() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        CircularProgressIndicator()
    }
}
fun readFilesNames(context: Context) {
    val filesDir = context.filesDir
    val files = filesDir.listFiles()

    for (file in files){
        Log.d("FILESDIR", "Filename is: ${file.name}")
    }
}

/*suspend fun downloadFile(url: String, fileName: String, context: Context) {
    withContext(Dispatchers.IO) {
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()


        val file = File(context.filesDir, fileName)
        file.outputStream().use { output ->
            response.body?.byteStream()?.use { input ->
                input.copyTo(output)
            }
        }

        val filesDirFile = readFile("weekly.xlsx", context)
        Log.d("DOWNLOAD", "Is file: ${filesDirFile.exists()}")

    }
}*/

fun readFile(fileName: String, context: Context): File {
    return File(context.filesDir, fileName)
}

@Composable
fun InterfaceDraw(context: Context, groupName: String){
    readFilesNames(context)
    var downloading by remember { mutableStateOf(true) }
    /*val coroutineScope = rememberCoroutineScope()
    val url = "https://docs.google.com/spreadsheets/d/1DkXND_5Q1OxGMXL5770-YKP_lOq5h8Jc/export?format=xlsx"

    LaunchedEffect(downloading) {
        coroutineScope.launch {
            downloadFile(url, "weekly.xlsx", context)
            downloading = false
        }
    }*/
    val file = readFile("weekly.xlsx", context)
    if (file.exists()) downloading = false

    Scaffold(
        topBar = {
            TopAppBar {
                Spacer(modifier = Modifier.weight(1f))
                Text(groupName, fontSize = 22.sp)
                Spacer(modifier = Modifier.weight(9f))
            }
        }
    ) {
        if (!downloading) 
        {
            val file = readFile("weekly.xlsx", context)
            if (file.exists()) {
                val wb: Workbook = WorkbookFactory.create(file)
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    ShowList(wb = wb, groupName = groupName)
                }
            } else {
                Column {
                    Text(text = "file doesn't exist")
                }
            }
            
        } else {
            Column {

            }
        }
    }
}
@Composable
fun ShowList(wb: Workbook, groupName: String){
    /*var parsing by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()
    var pair: Pair<MutableList<MutableList<LessonModel>>, MutableList<String>> = Pair(mutableListOf(), mutableListOf())
    DisposableEffect(parsing) {
        val job = coroutineScope.launch {
            pair = doAsync(wb, groupName)
            Log.d("ASYNC_DEBUG", "pair is ${pair.first}")
            Log.d("ASYNC_DEBUG", "pair2 is ${pair.second}")
            parsing = false
        }
        onDispose {
            job.cancel()
        }
    }
    if (parsing){
        Text(text = "parsing is in process")
    }
    else {
        val (fullLessonsInfo, datesInfo) = Pair(pair.first, pair.second)*/
    val (fullLessonsInfo, datesInfo) = Pair(algorithm(wb, groupName).first, algorithm(wb,groupName).second)

    for (i in 0 until 6){
        Row {
            Text(text = datesInfo[i])
        }
        for (lesson in fullLessonsInfo[i]){
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .padding(top = 2.dp)){
                Box(modifier = Modifier
                    .fillMaxHeight()
                    .background(color = Color.Gray)
                    .weight(1f)) {
                    Text(text = lesson.lessonNumber.toString(),
                        modifier = Modifier
                            .align(alignment = Alignment.Center))
                }
                Box(modifier = Modifier
                    .fillMaxHeight()
                    .padding(start = 2.dp)
                    .background(color = Color.Gray)
                    .weight(7f)){
                    Text(text = if (lesson.lessonName.size > 1) "Занятие по группам" else lesson.lessonName[0],
                        modifier = Modifier
                            .align(alignment = Alignment.CenterStart)
                            .padding(3.dp))
                }
                Box(modifier = Modifier
                    .fillMaxHeight()
                    .padding(start = 2.dp)
                    .background(color = Color.Gray)
                    .weight(1f)) {
                    Text(text = lesson.classroom[0],
                        modifier = Modifier
                            .align(alignment = Alignment.Center))
                }
            }
        }
    }
}