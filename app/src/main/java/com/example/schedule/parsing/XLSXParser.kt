package com.example.schedule.parsing

import com.example.schedule.LessonModel
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.util.CellAddress

fun algorithm(wb: Workbook, groupName: String): Pair<MutableList<MutableList<LessonModel>>, MutableList<String>> {
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
        val parsedLessonsInfo = getLessonPositions(sheet)

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
                val (rowIndex, difference, value) = Triple(lesson.first, lesson.second, lesson.third)

                // Iterate over each group of cells for the current lesson
                for (i in 0 until difference step 2) {
                    // Add the lesson name, teacher name, and classroom to their respective lists
                    lessonNamesList.add(getDataFromCell(rowIndex + i, columnNum, sheet, formatter))
                    teacherNamesList.add(getDataFromCell(rowIndex + 1 + i, columnNum, sheet, formatter))
                    classroomsList.add(getDataFromCell(rowIndex + i, columnNum + 1, sheet, formatter))

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

/*
private fun getLessonPositions(sheet: Sheet): List<List<LessonCellGroup>> {
    val lessonCellGroups = mutableListOf<LessonCellGroup>()

    for (mergedRegion in sheet.mergedRegions) {
        if (mergedRegion.firstColumn != mergedRegion.lastColumn) {
            continue
        }
        val firstCell = sheet.getRow(mergedRegion.firstRow)?.getCell(0)
        if (firstCell?.cellTypeEnum != CellType.NUMERIC) {
            continue
        }
        val lessonNumber = firstCell.numericCellValue.toInt()
        lessonCellGroups.add(LessonCellGroup(mergedRegion.firstRow, mergedRegion.numberOfCells, lessonNumber))
    }

    val weekLessonsIndexes = mutableListOf<List<LessonCellGroup>>()
    var dayLessonsIndexes = mutableListOf<LessonCellGroup>()

    lessonCellGroups.sortedBy { it.rowIndex }
        .forEachIndexed { index, lessonCellGroup ->
            dayLessonsIndexes.add(lessonCellGroup)
            if (index == lessonCellGroups.lastIndex || lessonCellGroup.lessonNumber >= lessonCellGroups[index + 1].lessonNumber) {
                weekLessonsIndexes.add(dayLessonsIndexes)
                dayLessonsIndexes = mutableListOf()
            }
        }

    return weekLessonsIndexes
}*/
