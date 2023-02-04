package com.example.schedule

import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.util.CellAddress

fun algorithm(wb: Workbook, groupName: String): Pair<MutableList<MutableList<LessonModel>>, MutableList<String>> {
    val formatter = DataFormatter()

    val lessonList = mutableListOf<MutableList<LessonModel>>()

    val dateList = mutableListOf<String>()
    for(sheet in wb){
        val columnNum = findColumn(sheet, groupName)
        if (columnNum == -1) continue

        val parsedLessonsInfo = getLessonPositions(sheet)

        for (day in parsedLessonsInfo){

            val dayLessonsList = mutableListOf<LessonModel>()

            val (index,_ , _) = Triple(day[0].first, day[0].second, day[0].third)
            dateList.add(getDataFromCell(index-3, 0, sheet, formatter))

            for (lesson in day){
                var lessonNamesList = mutableListOf<String>()
                var teacherNamesList = mutableListOf<String>()
                var classroomsList = mutableListOf<String>()
                val(rowIndex, difference, value) = Triple(lesson.first, lesson.second, lesson.third)

                for (i in 0 until difference step 2){
                    lessonNamesList.add(getDataFromCell(rowIndex + i, columnNum, sheet, formatter))
                    teacherNamesList.add(getDataFromCell(rowIndex + 1 + i, columnNum, sheet, formatter))
                    classroomsList.add(getDataFromCell(rowIndex + i, columnNum + 1, sheet, formatter))
                    
                    if (i == difference - 2){

                        if (lessonNamesList[0] == ""){
                            lessonNamesList = mutableListOf()
                            teacherNamesList = mutableListOf()
                            classroomsList = mutableListOf()

                            lessonNamesList.add("")
                            teacherNamesList.add("")
                            classroomsList.add("")
                        }

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
            lessonList.add(dayLessonsList)
        }
        return Pair(lessonList, dateList)
    }
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
