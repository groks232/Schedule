package com.example.schedule

import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.util.CellAddress

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
