package com.example.schedule.parsing

import android.util.Log
import androidx.compose.runtime.remember
import com.example.schedule.LessonModel
import org.apache.poi.ss.usermodel.*
import org.apache.poi.ss.util.CellAddress
import org.apache.poi.ss.util.CellRangeUtil
import org.apache.poi.ss.util.CellUtil
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileInputStream

fun algorithm(
    wb: Workbook,
    groupName: String
): Pair<MutableList<MutableList<LessonModel>>, MutableList<String>> {
    // Create a DataFormatter object to format cell values as strings
    val formatter = DataFormatter()

    // Create a list to store the lessons for each day
    val lessonList = mutableListOf<MutableList<LessonModel>>()

    // Create a list to store the dates for each day
    val dateList = mutableListOf<String>()

    // Iterate over each sheet in the workbook
    for (sheet in wb) {
        // Find the column number for the specified group name
        val columnNum = findColumn(sheet, groupName)

        // If the column number is not found, skip to the next sheet
        if (columnNum == -1) {
            continue
        }

        // Get the positions of all the lessons in the sheet
        val parsedLessonsInfo = mergeLessonNumberCells(sheet)

        // Iterate over each day in the parsed lesson info
        for (day in parsedLessonsInfo) {
            // Create a list to store the lessons for the current day
            val dayLessonsList = mutableListOf<LessonModel>()

            // Get the date for the current day and add it to the date list
            val (index, _, _) = Triple(day[0].first, day[0].second, day[0].third)
            dateList.add(getDataFromCell(index - 3, 0, sheet, formatter))

            // Iterate over each lesson in the current day
            for (lesson in day) {
                // Create lists to store the lesson name, teacher name, and classroom for the current lesson
                var lessonNamesList = mutableListOf<String>()
                var teacherNamesList = mutableListOf<String>()
                var classroomsList = mutableListOf<String>()

                // Get the row index, difference, and value for the current lesson
                val (rowIndex, difference, value) = Triple(
                    lesson.first,
                    lesson.second,
                    lesson.third
                )

                // Iterate over each group of cells for the current lesson
                for (i in 0 until difference step 2) {
                    // Add the lesson name, teacher name, and classroom to their respective lists
                    lessonNamesList.add(getDataFromCell(rowIndex + i, columnNum, sheet, formatter))
                    teacherNamesList.add(
                        getDataFromCell(
                            rowIndex + 1 + i,
                            columnNum,
                            sheet,
                            formatter
                        )
                    )
                    classroomsList.add(
                        getDataFromCell(
                            rowIndex + i,
                            columnNum + 1,
                            sheet,
                            formatter
                        )
                    )

                    // If this is the last group of cells for the current lesson
                    if (i == difference - 2) {
                        // If the lesson name is empty, create an empty lesson
                        if (lessonNamesList[0] == "") {
                            lessonNamesList = mutableListOf()
                            teacherNamesList = mutableListOf()
                            classroomsList = mutableListOf()

                            lessonNamesList.add("")
                            teacherNamesList.add("")
                            classroomsList.add("")
                        }

                        // Create a new LessonModel object and add it to the dayLessonsList
                        val lessonToAdd = LessonModel(
                            value,
                            lessonNamesList,
                            teacherNamesList,
                            classroomsList,
                        )
                        dayLessonsList.add(lessonToAdd)
                    }
                }
            }

            // Add the dayLessonsList to the lessonList
            lessonList.add(dayLessonsList)
        }
    }

    // Return the lessonList and dateList as a pair
    return Pair(lessonList, dateList)
}

private fun findColumn(sheet: Sheet, cellContent: String): Int {
    for (row in sheet) {
        for (cell in row) {
            if (cell.cellTypeEnum == CellType.STRING) {
                if (cell.richStringCellValue.string.trim { it <= ' ' } == cellContent) {
                    return cell.columnIndex
                }
            }
        }
    }
    return -1
}

private fun getDataFromCell(
    rowIndex: Int,
    columnIndex: Int,
    sheet: Sheet,
    formatter: DataFormatter
): String {
    if (rowIndex == -1 || columnIndex == -1) return "incorrect data"
    val cellAddress = CellAddress(rowIndex, columnIndex)
    val row = sheet.getRow(cellAddress.row)
    return formatter.formatCellValue(row.getCell(cellAddress.column))
}

fun mergeLessonNumberCells(sheet: Sheet): MutableList<MutableList<Triple<Int, Int, Int>>> {
    val dayStartRows = getFullyMergedRows(sheet)
    dayStartRows.sort()

    var dayStartRowListIndex = 0

    var dayStartRowContent = dayStartRows[dayStartRowListIndex]


    val lengthList = mutableListOf<Triple<Int, Int, Int>>()

    var lessonNumberLength = 0

    var previousCellValue = 0

    var previousRowIndex = 0


    outer@while(true) {
        for (rowIndex in dayStartRowContent + 3..sheet.lastRowNum) {
            val row = sheet.getRow(rowIndex)
            val cell = row.getCell(row.firstCellNum.toInt(), Row.MissingCellPolicy.CREATE_NULL_AS_BLANK)


            if (cell.cellTypeEnum == CellType.BLANK) {
                lessonNumberLength++
            }

            if (cell.cellTypeEnum == CellType.NUMERIC) {
                if (lessonNumberLength > 0) {

                    //first index, second length, third content
                    lengthList.add(
                        Triple(
                            previousRowIndex,
                            lessonNumberLength + 1,
                            //cell.numericCellValue.toInt()
                            previousCellValue
                        )
                    )
                    lessonNumberLength = 0
                    previousCellValue = cell.numericCellValue.toInt()
                    previousRowIndex = rowIndex
                    continue
                }
                else {
                    lessonNumberLength = 1
                    previousCellValue = cell.numericCellValue.toInt()
                    previousRowIndex = rowIndex
                    val a: Int
                }
            }

            if (dayStartRowListIndex < 5){
                if (dayStartRows[dayStartRowListIndex + 1] - rowIndex <= 2) {
                    dayStartRowListIndex++
                    dayStartRowContent = dayStartRows[dayStartRowListIndex]
                    break
                }
            }

            else {
                if (rowIndex == sheet.lastRowNum) break@outer
            }
        }
    }

    var dailyLengthList = mutableListOf<Triple<Int, Int, Int>>()
    val weeklyLengthList = mutableListOf<MutableList<Triple<Int, Int, Int>>>()

    for (item in 0 until lengthList.size - 1){
        if (lengthList[item].third < lengthList[item + 1].third) dailyLengthList.add(lengthList[item])
        else {
            dailyLengthList.add(lengthList[item + 1])
            weeklyLengthList.add(dailyLengthList)
            dailyLengthList = mutableListOf()
        }
    }
    dailyLengthList.add(lengthList.last())
    weeklyLengthList.add(dailyLengthList)

    return weeklyLengthList
}

fun getFullyMergedRows(sheet: Sheet): MutableList<Int> {
    val fullyMergedRows = mutableListOf<Int>()
    val mergedRegions = sheet.mergedRegions
    for (mergedRegion in mergedRegions) {
        if (mergedRegion.firstRow == mergedRegion.lastRow && mergedRegion.firstColumn == 0 && mergedRegion.lastColumn == 12) {
            fullyMergedRows.add(mergedRegion.firstRow)
        }
    }
    return fullyMergedRows.asReversed()
}