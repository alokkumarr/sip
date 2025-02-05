package sncr.bda.conf;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Generated;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * Parser specific properties
 */
@Generated("org.jsonschema2pojo")
public class Parser {

    @SerializedName("file")
    @Expose
    private String file;

    @SerializedName("parserInputFileFormat")
    @Expose
    private ParserInputFileFormat parserInputFileFormat = ParserInputFileFormat.fromValue("csv");

    @SerializedName("schemaValidationRequired")
    @Expose
    private Boolean schemaValidationRequired = false;

    @SerializedName("fields")
    @Expose
    private List<Field> fields = new ArrayList<Field>();

    @SerializedName("lineSeparator")
    @Expose
    private String lineSeparator;

    @SerializedName("delimiter")
    @Expose
    private String delimiter;

    @SerializedName("quoteChar")
    @Expose
    private String quoteChar;

    @SerializedName("quoteEscape")
    @Expose
    private String quoteEscape;

    @SerializedName("multiLine")
    @Expose
    private boolean multiLine;

    @SerializedName("headerSize")
    @Expose
    private Integer headerSize;

    @SerializedName("fieldDefRowNumber")
    @Expose
    int fieldDefRowNumber;

    /**
     * Number of files for all output objects
     */
    @SerializedName("numberOfFiles")
    @Expose
    private Integer numberOfFiles = 1;

    @SerializedName("isFlatteningEnabled")
    @Expose
    private boolean isFlatteningEnabled = false;

    @SerializedName("pivotFields")
    @Expose
    private PivotFields pivotFields = null;

    /**
     * Alert configuration for the component.
     */
    @SerializedName("alert")
    @Expose
    private Alert alerts;

    @SerializedName("allowInconsistentCol")
    @Expose
    private boolean allowInconsistentCol;

    @SerializedName("suppressDateAttrCols")
    @Expose
    private boolean suppressDateAttrCols = false;

    /**
     * No args constructor for use in serialization
     */
    public Parser() {
    }

    /**
     * @param file
     * @param fields
     * @param lineSeparator
     * @param delimiter
     * @param quoteChar
     * @param quoteEscape
     * @param headerSize
     * @param fieldDefRowNumber
     * @param numberOfFiles
     * @param multiLine
     * @param isFlatteningEnabled
     * @param pivotFields
     * @param alerts
     * @param allowInconsistentCol
     */
    public Parser(
        String file,
        List<Field> fields,
        String lineSeparator,
        String delimiter,
        String quoteChar,
        String quoteEscape,
        Integer headerSize,
        int fieldDefRowNumber,
        Integer numberOfFiles,
        boolean multiLine,
        boolean isFlatteningEnabled,
        PivotFields pivotFields,
        Alert alerts,
        boolean allowInconsistentCol,
        boolean suppressDateAttrCols) {
        this.file = file;
        this.fields = fields;
        this.lineSeparator = lineSeparator;
        this.delimiter = delimiter;
        this.quoteChar = quoteChar;
        this.quoteEscape = quoteEscape;
        this.headerSize = headerSize;
        this.fieldDefRowNumber = fieldDefRowNumber;
        this.numberOfFiles = numberOfFiles;
        this.multiLine = multiLine;
        this.isFlatteningEnabled = isFlatteningEnabled;
        this.pivotFields = pivotFields;
        this.alerts = alerts;
        this.allowInconsistentCol = allowInconsistentCol;
        this.suppressDateAttrCols = suppressDateAttrCols;
    }

    /**
     * @return The file
     */
    public String getFile() {
        return file;
    }

    /**
     * @param file The file
     */
    public void setFile(String file) {
        this.file = file;
    }

    public Parser withFile(String file) {
        this.file = file;
        return this;
    }

    public ParserInputFileFormat getParserInputFileFormat() {
        return this.parserInputFileFormat;
    }

    public void setParserInputFileFormat(ParserInputFileFormat fileFormat) {
        this.parserInputFileFormat = fileFormat;
    }

    public Parser withParserInputFileFormat(ParserInputFileFormat parserInputFileFormat) {
        this.parserInputFileFormat = parserInputFileFormat;

        return this;
    }

    public Boolean getSchemaValidationRequired() {
        return schemaValidationRequired;
    }

    public void setSchemaValidationRequired(Boolean schemaValidationRequired) {
        this.schemaValidationRequired = schemaValidationRequired;
    }

    public Parser withSchemaValidationRequired(Boolean schemaValidationRequired) {
        this.schemaValidationRequired = schemaValidationRequired;

        return this;
    }

    /**
     * @return The fields
     */
    public List<Field> getFields() {
        return fields;
    }

    /**
     * @param fields The fields
     */
    public void setFields(List<Field> fields) {
        this.fields = fields;
    }

    public Parser withFields(List<Field> fields) {
        this.fields = fields;
        return this;
    }

    /**
     * @return The lineSeparator
     */
    public String getLineSeparator() {
        return lineSeparator;
    }

    /**
     * @param lineSeparator The lineSeparator
     */
    public void setLineSeparator(String lineSeparator) {
        this.lineSeparator = lineSeparator;
    }

    public Parser withLineSeparator(String lineSeparator) {
        this.lineSeparator = lineSeparator;
        return this;
    }

    /**
     * @return The delimiter
     */
    public String getDelimiter() {
        return delimiter;
    }

    /**
     * @param delimiter The delimiter
     */
    public void setDelimiter(String delimiter) {
        this.delimiter = delimiter;
    }

    public Parser withDelimiter(String delimiter) {
        this.delimiter = delimiter;
        return this;
    }

    /**
     * @return The quoteChar
     */
    public String getQuoteChar() {
        return quoteChar;
    }

    /**
     * @param quoteChar The quoteChar
     */
    public void setQuoteChar(String quoteChar) {
        this.quoteChar = quoteChar;
    }

    public Parser withQuoteChar(String quoteChar) {
        this.quoteChar = quoteChar;
        return this;
    }

    /**
     * @return The quoteEscape
     */
    public String getQuoteEscape() {
        return quoteEscape;
    }

    /**
     * @param quoteEscape The quoteEscape
     */
    public void setQuoteEscape(String quoteEscape) {
        this.quoteEscape = quoteEscape;
    }

    public Parser withQuoteEscape(String quoteEscape) {
        this.quoteEscape = quoteEscape;
        return this;
    }

    /**
     * @return The headerSize
     */
    public Integer getHeaderSize() {
        return headerSize;
    }

    /**
     * @param headerSize The headerSize
     */
    public void setHeaderSize(Integer headerSize) {
        this.headerSize = headerSize;
    }

    public Parser withHeaderSize(Integer headerSize) {
        this.headerSize = headerSize;
        return this;
    }

    public int getFieldDefRowNumber() {
        return fieldDefRowNumber;
    }

    public void setFieldDefRowNumber(int fieldDefRowNumber) {
        this.fieldDefRowNumber = fieldDefRowNumber;
    }

    public Parser withFieldDefRowNumber(int fieldDefRowNumber) {
        this.fieldDefRowNumber = fieldDefRowNumber;
        return this;
    }

    /**
     * Number of files for all output objects
     *
     * @return The numberOfFiles
     */
    public Integer getNumberOfFiles() {
        return numberOfFiles;
    }

    /**
     * Number of files for all output objects
     *
     * @param numberOfFiles The numberOfFiles
     */
    public void setNumberOfFiles(Integer numberOfFiles) {
        this.numberOfFiles = numberOfFiles;
    }

    public Parser withNumberOfFiles(Integer numberOfFiles) {
        this.numberOfFiles = numberOfFiles;
        return this;
    }

    /**
     * Gets alerts
     *
     * @return value of alerts
     */
    public Alert getAlerts() {
        return alerts;
    }

    /**
     * Sets alerts
     */
    public void setAlerts(Alert alerts) {
        this.alerts = alerts;
    }

    public Parser withAlerts(Alert alerts) {
        this.alerts = alerts;
        return this;
    }

    /**
     * Gets inconsistentCol
     *
     * @return value of inconsistentCol
     */
    public boolean getAllowInconsistentColumn() {
        return allowInconsistentCol;
    }

    /**
     * Sets inconsistentCol
     */
    public void setAllowInconsistentColumn(boolean allowInconsistentCol) {
        this.allowInconsistentCol = allowInconsistentCol;
    }

    public Parser withAllowInconsistentColumn(boolean allowInconsistentCol) {
        this.allowInconsistentCol = allowInconsistentCol;
        return this;
    }

    public PivotFields getPivotFields() {
        return pivotFields;
    }

    public void setPivotFields(PivotFields pivotFields) {
        this.pivotFields = pivotFields;
    }

    public Parser withPivotFields(PivotFields pivotFields) {
        this.pivotFields = pivotFields;
        return this;
    }

    public boolean isMultiLine() {
        return multiLine;
    }

    public void setMultiLine(boolean multiLine) {
        this.multiLine = multiLine;
    }

    public Parser withMultiLine(boolean multiLine) {
        this.multiLine = multiLine;
        return this;
    }

    public boolean isFlatteningEnabled() {
        return isFlatteningEnabled;
    }

    public void setFlatteningEnabled(boolean flatteningEnabled) {
        isFlatteningEnabled = flatteningEnabled;
    }

    public Parser withFlatteningEnabled(boolean flatteningEnabled) {
        isFlatteningEnabled = flatteningEnabled;
        return this;
    }

    public boolean isSuppressDateAttrCols() {
        return suppressDateAttrCols;
    }

    public void setSuppressDateAttrCols(boolean suppressDateAttrCols) {
        this.suppressDateAttrCols = suppressDateAttrCols;
    }

    public Parser withSuppressDateAttrCols(boolean suppressDateAttrCols) {
        this.suppressDateAttrCols = suppressDateAttrCols;
        return this;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(file)
            .append(parserInputFileFormat)
            .append(schemaValidationRequired)
            .append(fields)
            .append(lineSeparator)
            .append(delimiter)
            .append(quoteChar)
            .append(quoteEscape)
            .append(headerSize)
            .append(fieldDefRowNumber)
            .append(numberOfFiles)
            .append(multiLine)
            .append(isFlatteningEnabled)
            .append(pivotFields)
            .append(alerts)
            .append(allowInconsistentCol)
            .append(suppressDateAttrCols)
            .toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Parser) == false) {
            return false;
        }
        Parser rhs = ((Parser) other);
        return new EqualsBuilder()
            .append(file, rhs.file)
            .append(parserInputFileFormat, rhs.parserInputFileFormat)
            .append(schemaValidationRequired, rhs.schemaValidationRequired)
            .append(fields, rhs.fields)
            .append(lineSeparator, rhs.lineSeparator)
            .append(delimiter, rhs.delimiter)
            .append(quoteChar, rhs.quoteChar)
            .append(quoteEscape, rhs.quoteEscape)
            .append(headerSize, rhs.headerSize)
            .append(fieldDefRowNumber, rhs.fieldDefRowNumber)
            .append(numberOfFiles, rhs.numberOfFiles)
            .append(multiLine, rhs.multiLine)
            .append(isFlatteningEnabled, rhs.isFlatteningEnabled)
            .append(pivotFields, rhs.pivotFields)
            .append(alerts, rhs.alerts)
            .append(allowInconsistentCol, this.allowInconsistentCol)
            .append(suppressDateAttrCols, rhs.suppressDateAttrCols)
            .isEquals();
    }
}
