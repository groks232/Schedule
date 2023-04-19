package com.example.schedule

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.schedule.ui.theme.ScheduleTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.regex.Pattern

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
                    GetFilesUrlsAndDownload(this)
                    Greeting()
                }
            }
        }
    }
}

@Composable
fun datesComparison(text1: String, text2: String): String {
    val dateFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    val pattern = Pattern.compile("\\d{2}\\.\\d{2}\\.\\d{4}")
    val matcher1 = pattern.matcher(text1)
    val matcher2 = pattern.matcher(text2)
    val date1 = if (matcher1.find()) LocalDate.parse(matcher1.group(), dateFormat) else null
    val date2 = if (matcher2.find()) LocalDate.parse(matcher2.group(), dateFormat) else null

    if (date1 != null && date2 != null) {
        if (date1.isBefore(date2)) {
            return "date2"
        } else if (date2.isBefore(date1)) {
            return  "date1"
        }
    } else {
        return "failed"
    }
    return "failed"
}
@Composable
fun GetFilesUrlsAndDownload(context: Context){
    var parsing by remember { mutableStateOf(true) }
    var downloading by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()
    var spreadsheetUrlsAndDatesList by remember { mutableStateOf(mutableListOf(Pair("", ""))) }

    var spreadsheetUrlToDownload by remember{ mutableStateOf("") }
    var documentUrlToDownload by remember{ mutableStateOf("") }

    LaunchedEffect(spreadsheetUrlsAndDatesList){
        Log.d("DOWNLOAD", "LaunchedEffect started")
        coroutineScope.launch {
            extractGoogleSpreadsheetUrlsFromSchedulePage(
                url = "https://kcpt72.ru/schedule/",
                onSuccess = { urls ->
                    spreadsheetUrlsAndDatesList = urls
                    urls.forEach { pair ->
                        val debugUrl = pair.first
                        val debugText = pair.second
                        Log.d("SPREADSHEET", "URL: $debugUrl, Text: $debugText")
                    }
                },
                onError = { errorMessage ->
                    Log.d("DOWNLOAD", "An error occurred: $errorMessage")
                }
            )
            extractGoogleDocumentUrlsFromSchedulePage(
                url = "https://kcpt72.ru/schedule/",
                onSuccess = {urls ->
                    for (debugUrl in urls) Log.d("DOCUMENT", debugUrl)
                    documentUrlToDownload = urls[0]
                },
                onError = { errorMessage ->
                    Log.d("DOWNLOAD", "An error occurred: $errorMessage")
                }
            )
            parsing = false
        }
    }

    if (!parsing){
        val dateChoose = datesComparison(text1 = spreadsheetUrlsAndDatesList[0].second, text2 = spreadsheetUrlsAndDatesList[1].second)

        val regexPatternForSpreadsheet = "^(https://docs.google.com/spreadsheets/d/[^/]+)/edit.*$".toRegex()
        val regexPatternForDocument = "^(https://docs.google.com/document/d/[^/]+)/edit.*$".toRegex()

        if (dateChoose == "date1") spreadsheetUrlToDownload = spreadsheetUrlsAndDatesList[0].first.replace(regexPatternForSpreadsheet, "$1/export?format=xlsx")
        else if (dateChoose == "date2") spreadsheetUrlToDownload = spreadsheetUrlsAndDatesList[1].first.replace(regexPatternForSpreadsheet, "$1/export?format=xlsx")

        documentUrlToDownload = documentUrlToDownload.replace(regexPatternForDocument, "$1/export?format=docx")

        LaunchedEffect(downloading) {
            GlobalScope.launch {
                downloadFile(spreadsheetUrlToDownload, "weekly.xlsx", context)
                downloadFile(documentUrlToDownload, "daily.xlsx", context)
                downloading = false
            }
        }
    }
}

suspend fun downloadFile(url: String, fileName: String, context: Context) {
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
}

suspend fun extractGoogleSpreadsheetUrlsFromSchedulePage(
    url: String,
    onSuccess: (MutableList<Pair<String, String>>) -> Unit,
    onError: (String) -> Unit
) {
    withContext(Dispatchers.IO) {
        try {
            val document = Jsoup.connect(url).get()
            val linkElements = document.select("a[href*=docs.google.com/spreadsheets]")

            if (linkElements.isNotEmpty()) {
                val urls = mutableListOf<Pair<String, String>>()
                val pattern = Pattern.compile("https://docs.google.com/spreadsheets/d/.*/edit.*")

                linkElements.forEach { linkElement ->
                    val linkHref = linkElement.attr("href")
                    val matcher = pattern.matcher(linkHref)

                    if (matcher.find()) {
                        val linkText = linkElement.text()
                        urls.add(Pair(matcher.group(), linkText))
                    }
                }
                if (urls.isNotEmpty()) {
                    onSuccess(urls)
                } else {
                    onError("Google Sheets URLs not found in links")
                }
            } else {
                onError("Links to Google Sheets not found on page")
            }
        } catch (e: Exception) {
            onError(e.message ?: "Unknown error")
        }
    }
}

suspend fun extractGoogleDocumentUrlsFromSchedulePage(url: String, onSuccess: (List<String>) -> Unit, onError: (String) -> Unit) {
    withContext(Dispatchers.IO) {
        try {
            val document = Jsoup.connect(url).get()
            val linkElements = document.select("a[href*=docs.google.com/document]")

            if (linkElements.isNotEmpty()) {
                val urls = mutableListOf<String>()
                val pattern = Pattern.compile("https://docs.google.com/document/d/.*/edit.*")

                linkElements.forEach { linkElement ->
                    val linkHref = linkElement.attr("href")
                    val matcher = pattern.matcher(linkHref)

                    if (matcher.find()) {
                        urls.add(matcher.group())
                    }
                }

                if (urls.isNotEmpty()) {
                    onSuccess(urls)
                } else {
                    onError("Google Docs URLs not found in links")
                }
            } else {
                onError("Links to Google Docs not found on page")
            }
        } catch (e: Exception) {
            onError(e.message ?: "Unknown error")
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
                        rows = listOf("20-11-1", "20-11-2", "21-11-1", "21-11-2", "22-11-1", "22-11-2")
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
                        rows = listOf("20-11-1", "20-11-2", "20-11-3", "20-11-4", "21-11-1", "21-11-2", "22-11-3", "22-11-1", "22-11-2", "22-11-3", "22-11-4")
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