package com.example.schedule

import android.content.ClipData
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.schedule.parsing.getDates
import com.example.schedule.parsing.algorithm
import com.example.schedule.ui.theme.ScheduleTheme
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
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

fun readFilesNames(context: Context) {
    val filesDir = context.filesDir
    val files = filesDir.listFiles()

    for (file in files){
        Log.d("FILESDIR", "Filename is: ${file.name}")
    }
}

fun readFile(fileName: String, context: Context): File {
    return File(context.filesDir, fileName)
}

@Composable
fun InterfaceDraw(context: Context, groupName: String){
    val scope = rememberCoroutineScope()
    var file: File? by remember { mutableStateOf(null) }
    var downloading by remember { mutableStateOf(true) }
    LaunchedEffect(downloading){
        scope.launch {
            if (file == null){
                while (file == null){
                    file = readFile("weekly.xlsx", context)
                }
            }
            else file = readFile("weekly.xlsx", context)
            downloading = false
        }
    }

    //if (file.exists()) downloading = false

    Scaffold(
        topBar = {
            TopAppBar {
                Spacer(modifier = Modifier.weight(1f))
                Text(groupName, fontSize = 22.sp)
                Spacer(modifier = Modifier.weight(9f))
            }
        }
    ) {

        if (!downloading) {
            val wb: Workbook = WorkbookFactory.create(file)
            val fullLessonsInfo = algorithm(wb, groupName)
            val datesInfo = getDates(wb)
            LessonsList(
                fullLessonsInfo,
                datesInfo,
                onItemClick = { lesson ->
                    for(lessonName in lesson.lessonName){
                        val toast = Toast.makeText(context, lessonName, Toast.LENGTH_SHORT)
                        toast.show()
                    }
                })
        } else {
            Column {
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)){
                    Text(text = "Parsing...",
                        fontSize = 30.sp,
                        modifier = Modifier
                            .align(Alignment.Center)
                    )
                }
            }
        }

    }
}
@Composable
fun LessonsList(fullLessonsInfo: MutableList<MutableList<LessonModel>>, datesInfo: MutableList<String>, onItemClick: (LessonModel) -> Unit) {
    // Flatten the list of lesson models to create a single list of items
    val items = mutableListOf<Any>()
    for ((index, dayLessons) in fullLessonsInfo.withIndex()) {
        // Add the date for the current day
        items.add(datesInfo[index])

        // Add each lesson for the current day
        for (lesson in dayLessons) {
            items.add(lesson)
        }
    }


    LazyColumn {
        items(items) { item ->
            // Depending on the type of item, create a view for either the date or the lesson
            when (item) {
                is String -> {
                    Row{
                        Text(text = item)
                    }
                }
                is LessonModel -> {
                    LessonView(lesson = item, onItemClick = onItemClick)
                }
            }
        }
    }

}

@Composable
fun LessonView(lesson: LessonModel, onItemClick: (LessonModel) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier
        .fillMaxWidth()
        .height(50.dp)
        .padding(top = 2.dp)
        .clickable { onItemClick(lesson) }
    ) {
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

