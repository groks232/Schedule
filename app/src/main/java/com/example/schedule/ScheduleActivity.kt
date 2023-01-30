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

fun algorithm(wb: Workbook, groupName: String): MutableList<LessonModel> {
    val formatter = DataFormatter()

    val lessonList = mutableListOf<LessonModel>()

    for(sheet in wb){
        val columnNum = findColumn(sheet, groupName)
        if (columnNum == -1) continue

        val newAlgorithmList = getLessonPositions(sheet)

        for (day in newAlgorithmList){
            loopLesson@for (lesson in day){
                val lessonNamesList = mutableListOf<String>()
                val teacherNamesList = mutableListOf<String>()
                val classroomsList = mutableListOf<String>()

                val(index, difference, value) = Triple(lesson.first, lesson.second, lesson.third)

                loopInLesson@for (i in 0 until difference step 2){
                    if (difference == 2){
                        lessonNamesList.add(getDataFromCell(index, columnNum, sheet, formatter))
                        teacherNamesList.add(getDataFromCell(index + 1, columnNum, sheet, formatter))
                        classroomsList.add(getDataFromCell(index, columnNum + 1, sheet, formatter))
                        val lessonToAdd = LessonModel(
                            value,
                            lessonNamesList,
                            teacherNamesList,
                            classroomsList,
                        )
                        lessonList.add(lessonToAdd)
                        continue@loopLesson
                    }

                    else if (difference > 2 && difference % 2 == 0){
                        if (getDataFromCell(index, columnNum, sheet, formatter) == ""){
                            lessonNamesList.add(getDataFromCell(index + i, columnNum, sheet, formatter))
                            teacherNamesList.add(getDataFromCell(index + 1 + i, columnNum, sheet, formatter))
                            classroomsList.add(getDataFromCell(index + i, columnNum + 1, sheet, formatter))

                            if (getDataFromCell(index + i, columnNum, sheet, formatter) == "") continue@loopLesson

                            val lessonToAdd = LessonModel(
                                value,
                                lessonNamesList,
                                teacherNamesList,
                                classroomsList,
                            )
                            lessonList.add(lessonToAdd)

                            continue@loopLesson
                        }
                        else {
                            lessonNamesList.add(getDataFromCell(index + i, columnNum, sheet, formatter))
                            teacherNamesList.add(getDataFromCell(index + 1 + i, columnNum, sheet, formatter))
                            classroomsList.add(getDataFromCell(index + i, columnNum + 1, sheet, formatter))

                            if (i != difference - 2) continue@loopInLesson
                            else{
                                val lessonToAdd = LessonModel(
                                    value,
                                    lessonNamesList,
                                    teacherNamesList,
                                    classroomsList,
                                )
                                lessonList.add(lessonToAdd)
                            }
                        }
                    }
                }
            }
        }
        return lessonList
    }
    return lessonList
}

private fun findColumn(sheet: Sheet, cellContent: String): Int {
    for (row in sheet) {
        for (cell in row) {
            if(cell.cellTypeEnum == CellType.STRING){
                if (cell.richStringCellValue.string.trim { it <= ' ' } == cellContent) {
                    return cell.columnIndex
                }
            }
        }
    }
    return -1
}

private fun getDataFromCell(rowIndex: Int, columnIndex: Int, sheet: Sheet, formatter: DataFormatter): String {
    if (rowIndex == -1 || columnIndex == -1) return "incorrect data"
    val cellAddress = CellAddress(rowIndex, columnIndex)
    val row = sheet.getRow(cellAddress.row)
    return formatter.formatCellValue(row.getCell(cellAddress.column))
}

private fun getLessonPositions(sheet: Sheet): MutableList<MutableList<Triple<Int, Int, Int>>> {
    val lengthsList = mutableListOf<Triple<Int, Int, Int>>()
    val listOfMergedCells = sheet.mergedRegions
    for (i in listOfMergedCells){
        if (i.firstColumn != i.lastColumn) continue
        if (i.firstColumn != 0) continue
        if (sheet.getRow(i.firstRow).getCell(0).cellTypeEnum != CellType.NUMERIC) continue
        lengthsList.add(Triple(i.firstRow, i.numberOfCells, sheet.getRow(i.firstRow).getCell(0).numericCellValue.toInt()))
    }

    val weekLessonsIndexes = mutableListOf<MutableList<Triple<Int, Int, Int>>>()
    var dayLessonsIndexes = mutableListOf<Triple<Int, Int, Int>>()

    lengthsList.reverse()
    lengthsList.sortBy { it.first }

    for (i in 0 until lengthsList.size){
        if (i == lengthsList.size - 1) {
            val (rowIndex, mergedCellsCount, lessonNumber) = Triple(lengthsList[i].first, lengthsList[i].second, lengthsList[i].third)
            dayLessonsIndexes.add(Triple(rowIndex, mergedCellsCount, lessonNumber))
            weekLessonsIndexes.add(dayLessonsIndexes)
            break
        }

        val (rowIndex, mergedCellsCount, lessonNumber) = Triple(lengthsList[i].first, lengthsList[i].second, lengthsList[i].third)
        val (rowIndexNext, mergedCellsCountNext, lessonNumberNext) = Triple(lengthsList[i + 1].first, lengthsList[i + 1].second, lengthsList[i + 1].third)

        if (lessonNumber < lessonNumberNext){
            dayLessonsIndexes.add(Triple(rowIndex, mergedCellsCount, lessonNumber))
        }
        else{
            dayLessonsIndexes.add(Triple(rowIndex, mergedCellsCount, lessonNumber))
            weekLessonsIndexes.add(dayLessonsIndexes)
            dayLessonsIndexes = mutableListOf()
        }
    }
    return weekLessonsIndexes
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
