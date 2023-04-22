package com.example.schedule.parsing

import com.example.schedule.LessonModel
import org.apache.poi.ss.usermodel.*
import org.apache.poi.ss.util.CellAddress
import org.apache.poi.xssf.usermodel.XSSFCellStyle
import org.apache.poi.xssf.usermodel.XSSFColor


private fun findColumn(sheet: Sheet, cellContent: String): Int {
    for (row in sheet) {
        for (cell in row) {
            if (cell.cellType == CellType.STRING) {
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

fun getLessonNumberCells(sheet: Sheet): MutableList<MutableList<Triple<Int, Int, Int>>> {
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

            if (cell.cellType == CellType.NUMERIC) {
                if (lessonNumberLength > 0) {
                    //first index, second length, third content
                    lengthList.add(
                        Triple(previousRowIndex,lessonNumberLength, previousCellValue)
                    )

                    lessonNumberLength = 1
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

            if (cell.cellType == CellType.BLANK) {
                lessonNumberLength++
            }
        }
    }

    var dailyLengthList = mutableListOf<Triple<Int, Int, Int>>()
    val weeklyLengthList = mutableListOf<MutableList<Triple<Int, Int, Int>>>()

    for (item in 0 until lengthList.size - 1){
        if (lengthList[item].third < lengthList[item + 1].third) dailyLengthList.add(lengthList[item])
        else {
            dailyLengthList.add(lengthList[item])
            weeklyLengthList.add(dailyLengthList)
            dailyLengthList = mutableListOf()
        }
    }
    dailyLengthList.add(lengthList.last())
    weeklyLengthList.add(dailyLengthList)

    return weeklyLengthList
}

fun algorithm(wb: Workbook, groupName: String): MutableList<MutableList<LessonModel>>{
    val weekLessonModel = mutableListOf<MutableList<LessonModel>>()
    for (sheet in wb){
        val columnNum = findColumn(sheet, groupName)
        // If the column number is not found, skip to the next sheet
        if (columnNum == -1) {
            continue
        }
        val weekLessonNumberCells = getLessonNumberCells(sheet)
        for (dayLessonNumberCells in weekLessonNumberCells){
            val dayLessonModel = mutableListOf<LessonModel>()
            for (lessonNumberCells in dayLessonNumberCells){
                val (rowIndex, length, lessonNumber) = lessonNumberCells
                val notEmptyCells = mutableListOf<Cell>()
                for (rowNum in rowIndex until rowIndex + length){
                    val row = sheet.getRow(rowNum)
                    val cell = row.getCell(columnNum, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK)
                    if (cell.cellType != CellType.BLANK){
                        notEmptyCells.add(cell)
                    }
                }
                if (notEmptyCells.isEmpty()){
                    val names = mutableListOf<String>()
                    val teachers = mutableListOf<String>()
                    val classrooms = mutableListOf<String>()
                    names.add("")
                    teachers.add("")
                    classrooms.add("")
                    val lesson = LessonModel(lessonNumber, names, teachers, classrooms)
                    dayLessonModel.add(lesson)
                }
                else {
                    val names = mutableListOf<String>()
                    val teachers = mutableListOf<String>()
                    val classrooms = mutableListOf<String>()
                    for (index in 0 until notEmptyCells.size){
                        if (index % 2 == 0) names.add(notEmptyCells[index].stringCellValue)
                        else teachers.add(notEmptyCells[index].stringCellValue)
                    }
                    for (rowNum in rowIndex until rowIndex + length){
                        val row = sheet.getRow(rowNum)
                        val cell = row.getCell(columnNum + 1, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK)
                        if (cell.cellType == CellType.NUMERIC){
                            val classNum = cell.numericCellValue
                            classrooms.add(classNum.toString())
                        }
                        if (cell.cellType == CellType.STRING){
                            val classNum = cell.stringCellValue
                            classrooms.add(classNum.toString())
                        }
                    }
                    val lesson = LessonModel(lessonNumber, names, teachers, classrooms)
                    dayLessonModel.add(lesson)
                }
            }
            weekLessonModel.add(dayLessonModel)
        }
    }
    return weekLessonModel
}

fun getFullyMergedRows(sheet: Sheet): MutableList<Int> {
    val fullyMergedRows = mutableListOf<Int>()
    val mergedRegions = sheet.mergedRegions

    val row: Row = sheet.getRow(6)
    val lastCellNum = row.lastCellNum.toInt() - 1

    for (mergedRegion in mergedRegions) {
        if (mergedRegion.firstRow == mergedRegion.lastRow && mergedRegion.firstColumn == 0 && mergedRegion.lastColumn == lastCellNum) {
            fullyMergedRows.add(mergedRegion.firstRow)
        }
    }
    return fullyMergedRows
}

fun getDates(wb: Workbook): MutableList<String>{
    val dates = mutableListOf<String>()
    val sheet = wb.getSheetAt(0)

    val list = getFullyMergedRows(sheet)

    for (rowNum in list){
        val row = sheet.getRow(rowNum)
        for (cell in row){
            if (cell == null) continue
            else {
                dates.add(cell.stringCellValue)
                break
            }
        }
    }

    return dates
}