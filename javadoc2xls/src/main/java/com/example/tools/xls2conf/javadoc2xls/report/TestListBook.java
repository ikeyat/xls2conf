package com.example.tools.xls2conf.javadoc2xls.report;

import com.example.tools.xls2conf.javadoc2xls.doc.ClassDocBean;
import com.example.tools.xls2conf.javadoc2xls.doc.MethodDocBean;
import com.example.tools.xls2conf.javadoc2xls.report.writer.*;
import com.example.tools.xls2conf.javadoc2xls.report.writer.once.*;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by ikeya on 15/01/18.
 */
public class TestListBook {
    private static final Logger logger = LoggerFactory.getLogger(TestListBook.class);
    public static final String MARKER_ROW = "#row";
    public static final String MARKER_CELL = "#cell";
    public static final String MARKER_CLASSNAME = "#className";

    private Workbook workbook;
    private Sheet sheet;
    private int sheetIndex;
    private Set<CellWriter> cellWriters;
    private Map<String, Cell> cellMap;
    private int rowIndex = -1;

    public TestListBook(String templateFile, int sheetIndex) throws IOException, InvalidFormatException {
        InputStream inputStream = new FileInputStream(templateFile);
        this.workbook = WorkbookFactory.create(inputStream);
        this.sheetIndex = sheetIndex;
        this.cellWriters = new HashSet<>();
        this.cellMap = new HashMap<>();
        this.sheet = workbook.getSheetAt(sheetIndex);
        seekHead();
    }

    /**
     * Search markers and set head position on the first #row marker.
     */
    public void seekHead() {
        for (Row row : this.sheet) {
            for (Cell cell : row) {
                String cellValue = cell.getStringCellValue();
                if (cellValue == null || cellValue.length() == 0) {
                    // skip blank cell
                    continue;
                }
                cellValue = cellValue.trim();
                if (cellValue.startsWith(MARKER_ROW)) {
                    // it begins with "#row"
                    // check which the marker is for tags or reserved word
                    String type = cellValue.replace(MARKER_ROW, "");
                    int columnIndex = cell.getColumnIndex();
                    CellWriter writer;
                    if (TestMethodNameCellWriter.isTestMethodNameType(type)) {
                        // for testMethodName
                        writer = new TestMethodNameCellWriter(columnIndex);
                    } else if (CommentTextCellWriter.isCommentTextType(type)) {
                        // for commentText
                        writer = new CommentTextCellWriter(columnIndex);
                    } else if (StaticTextCellWriter.isStaticTextType(type)) {
                        // for staticText
                        writer = new StaticTextCellWriter(type, columnIndex);
                    } else if (DateCellWriter.isDateType(type)) {
                        // for date
                        writer = new DateCellWriter(type, columnIndex);
                    } else {
                        // for tags
                        writer = new TagCellWriter(type, columnIndex);
                    }

                    cellWriters.add(writer);
                    rowIndex = cell.getRowIndex();

                    logger.debug("row marker \"{}\" is detected at ({}, {})", type, columnIndex, rowIndex);
                }
            }
        }
    }

    public void writeCell(ClassDocBean classDocBean) {
        for (Row row : this.sheet) {
            for (Cell cell : row) {
                String cellValue = cell.getStringCellValue();
                if (cellValue == null || cellValue.length() == 0) {
                    // skip blank cell
                    continue;
                }
                cellValue = cellValue.trim();
                if (cellValue.startsWith(MARKER_CELL)) {
                    String type = cellValue.replace(MARKER_CELL, "");
                    logger.debug("cell marker \"{}\" is detected at ({}, {})", type, cell.getColumnIndex(),
                            cell.getRowIndex());
                    OnceCellWriter writer;
                    if (ClassNameOnceCellWriter.isTestMethodNameType(type)) {
                        // for classdName
                        writer = new ClassNameOnceCellWriter();
                    } else if (CommentTextOnceCellWriter.isCommentTextType(type)) {
                        // for commentText
                        writer = new CommentTextOnceCellWriter();
                    } else if (DateOnceCellWriter.isDateType(type)) {
                        // for date
                        writer = new DateOnceCellWriter(type);
                    } else {
                        // for tags
                        writer = new TagOnceCellWriter(type);
                    }
                    writer.write(cell, classDocBean);
                }
            }
        }
    }

    public void writeRow(MethodDocBean methodDocBean) {
        Row row = sheet.getRow(rowIndex);
        if (row == null) {
            row = sheet.createRow(rowIndex);
        }
        for (CellWriter writer : cellWriters) {
            writer.write(row, methodDocBean);
        }
        rowIndex++;
    }

    public void saveToFile(OutputStream outputStream) throws IOException {
        workbook.write(outputStream);
    }
}
