package com.example.schedule

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.schedule.ui.theme.ScheduleTheme
import org.apache.poi.ss.usermodel.*
import org.apache.poi.ss.util.CellAddress
import org.apache.poi.ss.util.CellRangeAddress

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
fun InterfaceDraw(context: Context, groupName: String){
    var pickedImageUri by remember { mutableStateOf<Uri?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        pickedImageUri = it.data?.data
    }
    pickedImageUri?.let {
        val inputStream = context.contentResolver.openInputStream(it)
        val wb = WorkbookFactory.create(inputStream)
        inputStream?.close()
        Column(modifier = Modifier
            .verticalScroll(state = rememberScrollState())) {

            ShowList(wb = wb, groupName = groupName)

        }
        wb.close()
    }
    Button(
        onClick = {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                .apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                }
            launcher.launch(intent)
        }
    ) {
        Text("Select")
    }
}

@Composable
fun ShowList(wb: Workbook, groupName: String){
    val fullLessonsInfo = algorithm(wb, groupName)
    for (i in 0 until fullLessonsInfo.size){
        if (i == fullLessonsInfo.size - 1) continue
        if (fullLessonsInfo[i].lessonNumber < fullLessonsInfo[i + 1].lessonNumber){
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .padding(top = 2.dp)){
                Box(modifier = Modifier
                    .fillMaxHeight()
                    .background(color = Color.Gray)
                    .weight(1f)) {
                    Text(text = fullLessonsInfo[i].lessonNumber.toString(),
                        modifier = Modifier
                            .align(alignment = Alignment.Center))
                }
                Box(modifier = Modifier
                    .fillMaxHeight()
                    .padding(start = 2.dp)
                    .background(color = Color.Gray)
                    .weight(7f)){
                    Text(text = if (fullLessonsInfo[i].lessonName.size > 1) "Занятие по группам" else fullLessonsInfo[i].lessonName[0],
                        modifier = Modifier
                            .align(alignment = Alignment.CenterStart)
                            .padding(3.dp))
                }
                Box(modifier = Modifier
                    .fillMaxHeight()
                    .padding(start = 2.dp)
                    .background(color = Color.Gray)
                    .weight(1f)) {
                    Text(text = fullLessonsInfo[i].classroom[0],
                        modifier = Modifier
                            .align(alignment = Alignment.Center))
                }
            }
        }
        else {
            Row{
                Text(text = "Day")
            }
        }
    }
}
