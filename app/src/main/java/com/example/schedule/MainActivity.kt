package com.example.schedule

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.schedule.ui.theme.ScheduleTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ScheduleTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    Greeting()
                }
            }
        }
    }
}

@Composable
fun Greeting() {
    Scaffold(
        topBar = {
            TopAppBar {
                Spacer(modifier = Modifier
                    //.width(20.dp)
                    .weight(1f))
                Text("Выбор группы", fontSize = 22.sp)
                Spacer(modifier = Modifier
                    //.width(20.dp)
                    .weight(9f))
            }
        }
    ) {
        Column{
            CollapsableLazyColumn(
                sections = listOf(
                    CollapsableSection(
                        title = "АТ",
                        rows = listOf("20-11", "21-11", "22-11")
                    ),
                    CollapsableSection(
                        title = "ДО",
                        rows = listOf("20-11-1", "20-11-2", "21-11-1", "21-11-2", "22-11-1", "21-11-2")
                    ),
                    CollapsableSection(
                        title = "ИБАС",
                        rows = listOf("21-11", "22-11")
                    ),
                    CollapsableSection(
                        title = "ИСиП",
                        rows = listOf("20-11-1", "20-11-2", "20-11-3", "21-11-1", "21-11-2", "21-11-3", "22-11-1", "22-11-2", "22-11-3")
                    ),
                    CollapsableSection(
                        title = "КП",
                        rows = listOf("20-11-1", "20-11-2", "20-11-3", "20-11-4", "21-11-1", "21-11-2", "22-11-3", "21-11-4", "22-11-1", "22-11-2", "22-11-3", "22-11-4")
                    ),
                    CollapsableSection(
                        title = "ОСАТПиП",
                        rows = listOf("20-11-1", "20-11-2", "21-11", "22-11")
                    ),
                    CollapsableSection(
                        title = "ПДО ТТ",
                        rows = listOf("20-11", "21-11", "22-11")
                    ),
                    CollapsableSection(
                        title = "ССА",
                        rows = listOf("20-11-1", "20-11-2", "20-11-3", "21-11-1", "21-11-2", "21-11-3", "22-11-1", "22-11-2")
                    ),
                ),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    ScheduleTheme {
        
    }
}

@Composable
fun CollapsableLazyColumn(
    sections: List<CollapsableSection>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val collapsedState = remember(sections) { sections.map { true }.toMutableStateList() }
    LazyColumn(modifier) {
        sections.forEachIndexed { i, dataItem ->
            val collapsed = collapsedState[i]
            item(key = "header_$i") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable {
                            collapsedState[i] = !collapsed
                        }
                ) {
                    Icon(
                        Icons.Default.run {
                            if (collapsed)
                                KeyboardArrowDown
                            else
                                KeyboardArrowUp
                        },
                        contentDescription = "",
                        tint = Color.LightGray,
                    )
                    Text(
                        dataItem.title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .padding(vertical = 10.dp)
                            .weight(1f)
                    )
                }
                Divider()
            }
            if (!collapsed) {
                items(dataItem.rows) { row ->
                    Row(modifier = Modifier
                        .clickable(onClick = {
                            val intent = Intent(context, ScheduleActivity::class.java)
                            intent.putExtra("groupNumber", row)
                            intent.putExtra("groupName", dataItem.title)
                            //startActivity(intent)
                            context.startActivity(intent)
                        })) {
                        Spacer(modifier = Modifier.size(MaterialIconDimension.dp))
                        Text(
                            row,
                            modifier = Modifier
                                .padding(vertical = 10.dp)
                        )
                        Spacer(modifier = Modifier
                            .fillMaxWidth())
                    }
                    Divider()
                }
            }
        }
    }
}

data class CollapsableSection(val title: String, val rows: List<String>)

const val MaterialIconDimension = 24f