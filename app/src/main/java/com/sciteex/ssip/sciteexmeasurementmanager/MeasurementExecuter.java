package com.sciteex.ssip.sciteexmeasurementmanager;

import com.annimon.stream.Stream;
import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Created by Gajos on 9/7/2017.
 */

public class MeasurementExecuter {

    //Patterns for change data from file
    private final static String ANY_SIGN_PATTERN = ".*";

    private final static String NAME_PATTERN_OPENING = "<name>";
    private final static String NAME_PATTERN_CLOSING = "</name>";

    private final static String MIN_BOUNDS_PATTERN_OPENING = "<min>";
    private final static String MIN_BOUNDS_PATTERN_CLOSING = "</min>";

    private final static String MAX_BOUNDS_PATTERN_OPENING = "<max>";
    private final static String MAX_BOUNDS_PATTERN_CLOSING = "</max>";

    //Variable tags.
    private final static String VARIABLES_PATTERN_OPENING = "<variables>";
    private final static String VARIABLES_PATTERN_CLOSING = "</variables>";

    //-----------------------------Variable types-------------------------------
    private final static String BOOLEAN_VARIABLE_PATTERN_OPENING = "<boolean>";
    private final static String BOOLEAN_VARIABLE_PATTERN_CLOSING = "</boolean>";

    private final static String STRING_VARIABLE_PATTERN_OPENING = "<string>";
    private final static String STRING_VARIABLE_PATTERN_CLOSING = "</string>";

    private final static String INTEGER_VARIABLE_PATTERN_OPENING = "<integer>";
    private final static String INTEGER_VARIABLE_PATTERN_CLOSING = "</integer>";

    private final static String FLOAT_VARIABLE_PATTERN_OPENING = "<float>";
    private final static String FLOAT_VARIABLE_PATTERN_CLOSING = "</float>";

    private final static String DOUBLE_VARIABLE_PATTERN_OPENING = "<double>";
    private final static String DOUBLE_VARIABLE_PATTERN_CLOSING = "</double>";
    //--------------------------------------------------------------------------

    private final static String NODE_PATTERN_OPENING = "<node>";
    private final static String NODE_PATTERN_CLOSING = "</node>";

    private final static String PATH_PATTERN_OPENING = "<path>";
    private final static String PATH_PATTERN_CLOSING = "</path>";

    private final static String X_POSITION_PATTERN_OPENING = "<x>";
    private final static String X_POSITION_PATTERN_CLOSING = "</x>";

    private final static String Y_POSITION_PATTERN_OPENING = "<y>";
    private final static String Y_POSITION_PATTERN_CLOSING = "</y>";

    private final static String SLIDE_PATTERN_OPENING = "<slide>";
    private final static String SLIDE_PATTERN_CLOSING = "</slide>";

    /**
     * Number of files with additional info.
     */
    private final static int ADDITIONAL_FILES_NUMBER = 2;

    /**
     * Boolean variable talking about state of additional information.
     */
    boolean isAdditionalInfo = false;

    /**
     * File with additional data.
     */
    private MeasurementAdditionalDataReader additionalInfo;

    /**
     * Zip file with program.
     */
    ZipFile zipFile;

    /**
     * Name of program.
     */
    private String programName;

    /**
     * Iterator int with actual picture number.
     */
    private int pictureNumber = -1;

    /**
     * List of entries in selected program.
     */
    private List<ZipEntry> entriesList;

    /**
     * List for measures bounds.
     */
    private List<MeasureBounds> measureBoundsList = new ArrayList<>();
    /**
     * Path to program.
     */
    private String programPath;

    /**
     * List with MovingPopups of this program.
     */
    private List<OpcUaVariable> opcUaVariables = new ArrayList<>();


    public List<OpcUaVariable> getOpcUaVariables()
    {
        return this.opcUaVariables;
    }

    /**
     * Method to open chosen program.
     * @throws IOException
     */
    public void openProgram(boolean isAdditionalInfo) throws IOException
    {
        if(isAdditionalInfo) {
            additionalInfo = new MeasurementAdditionalDataReader();
            additionalInfo.setProgramPath(programPath);
            additionalInfo.open();
            this.isAdditionalInfo = true;
        }
        openProgram();
    }

    /**
     * Method to open program from file.
     * @throws IOException
     */
    public void openProgram() throws IOException
    {
        //Get files from "program" - zip archive.
        zipFile = new ZipFile(programPath);

        Enumeration<? extends ZipEntry> entries = zipFile.entries();

        //Put entries to list.
        entriesList = new ArrayList<>();
        while(entries.hasMoreElements()){
            ZipEntry entry = entries.nextElement();
            entriesList.add(entry);
        }

        //Sort list by file name.
        Stream.of(entriesList)
                .sortBy(ZipEntry::getName);

        //Get program name from file
        getControlDataFromFile();

        //Get measures from file
        getMeasuresBoundsFromFile();
    }

    /**
     * Method to get picture number.
     * @return pictureNumber
     */
    public int getSlideNumber()
    {
        return this.pictureNumber;
    }

    private int findInZipByName(String name)
    {
        return entriesList.lastIndexOf(zipFile.getEntry(name));
    }

    private void getControlDataFromFile() throws IOException
    {
        //Last file of entries in zip archive is "title" file with program name

        InputStream nameStream = zipFile.getInputStream(entriesList.get(findInZipByName("config")));
        String controlFileContent =  CharStreams.toString(new InputStreamReader(nameStream, Charsets.UTF_8));

        //Get program name.
        Stream.of(controlFileContent)
                .map(s -> addProgramName(s))
                .map(s -> addOpcUaVariablesDefinitions(s))
                .forEach(s -> addBounds(s));
    }

    private String addProgramName(String content) {
        String regex = NAME_PATTERN_OPENING + ANY_SIGN_PATTERN + NAME_PATTERN_CLOSING;
        Pattern namePattern = Pattern.compile(regex);
        Matcher nameMatcher = namePattern.matcher(content);
        if(nameMatcher.find())
        {
            programName = nameMatcher.group()
                    .replace(NAME_PATTERN_OPENING,"")
                    .replace(NAME_PATTERN_CLOSING, "");

        }
        return nameMatcher.replaceFirst(regex);
    }

    private String addOpcUaVariablesDefinitions(String content) {

        //Init patterns.
        String variableRegex = VARIABLES_PATTERN_OPENING + ANY_SIGN_PATTERN + VARIABLES_PATTERN_CLOSING;
        Pattern variablesPattern = Pattern.compile(variableRegex);
        Matcher variablesMatcher = variablesPattern.matcher(content);

        String booleanRegex = BOOLEAN_VARIABLE_PATTERN_OPENING + ANY_SIGN_PATTERN + BOOLEAN_VARIABLE_PATTERN_CLOSING;
        Pattern booleanPattern = Pattern.compile(booleanRegex);

        String integerRegex = INTEGER_VARIABLE_PATTERN_OPENING   + ANY_SIGN_PATTERN + INTEGER_VARIABLE_PATTERN_CLOSING;
        Pattern integerPattern = Pattern.compile(integerRegex);

        String floatRegex = FLOAT_VARIABLE_PATTERN_OPENING   + ANY_SIGN_PATTERN + FLOAT_VARIABLE_PATTERN_CLOSING;
        Pattern floatPattern = Pattern.compile(floatRegex);

        String doubleRegex = DOUBLE_VARIABLE_PATTERN_OPENING   + ANY_SIGN_PATTERN + DOUBLE_VARIABLE_PATTERN_CLOSING;
        Pattern doublePattern = Pattern.compile(doubleRegex);

        String stringRegex = STRING_VARIABLE_PATTERN_OPENING   + ANY_SIGN_PATTERN + STRING_VARIABLE_PATTERN_CLOSING;
        Pattern stringPattern = Pattern.compile(stringRegex);

        if(variablesMatcher.find())
        {
            String variablesString = variablesMatcher.group()
                    .replace(VARIABLES_PATTERN_OPENING,"")
                    .replace(VARIABLES_PATTERN_CLOSING, "");

            //Find all variables.
            //Boolean
            Matcher booleanMatcher = booleanPattern.matcher(variablesString);
            while(booleanMatcher.find())
            {
                OpcUaVariable newVariable = new OpcUaVariable();
                newVariable.setType('b');
                variablesString = Stream.of(booleanMatcher.group())
                        .map(s -> addNode(newVariable, s))
                        .map(s -> addPath(newVariable, s))
                        .map(s -> addPosition(newVariable, s))
                        .map(s -> addSlide(newVariable, s))
                        .single();
                opcUaVariables.add(newVariable);
            }

            //Integer
            Matcher integerMatcher = integerPattern.matcher(variablesString);
            while(integerMatcher.find())
            {
                OpcUaVariable newVariable = new OpcUaVariable();
                newVariable.setType('i');
                variablesString = Stream.of(integerMatcher.group())
                        .map(s -> addNode(newVariable, s))
                        .map(s -> addPath(newVariable, s))
                        .map(s -> addPosition(newVariable, s))
                        .map(s -> addSlide(newVariable, s))
                        .single();
                opcUaVariables.add(newVariable);
            }

            //Float
            Matcher floatMatcher = floatPattern.matcher(variablesString);
            while(floatMatcher.find())
            {
                OpcUaVariable newVariable = new OpcUaVariable();
                newVariable.setType('f');
                variablesString = Stream.of(floatMatcher.group())
                        .map(s -> addNode(newVariable, s))
                        .map(s -> addPath(newVariable, s))
                        .map(s -> addPosition(newVariable, s))
                        .map(s -> addSlide(newVariable, s))
                        .single();
                opcUaVariables.add(newVariable);
            }

            //Double
            Matcher doubleMatcher = doublePattern.matcher(variablesString);
            while(doubleMatcher.find())
            {
                OpcUaVariable newVariable = new OpcUaVariable();
                newVariable.setType('d');
                variablesString = Stream.of(doubleMatcher.group())
                        .map(s -> addNode(newVariable, s))
                        .map(s -> addPath(newVariable, s))
                        .map(s -> addPosition(newVariable, s))
                        .map(s -> addSlide(newVariable, s))
                        .single();
                opcUaVariables.add(newVariable);
            }

            //String
            Matcher stringMatcher = stringPattern.matcher(variablesString);
            while(stringMatcher.find())
            {
                OpcUaVariable newVariable = new OpcUaVariable();
                newVariable.setType('s');
                variablesString = Stream.of(floatMatcher.group())
                        .map(s -> addNode(newVariable, s))
                        .map(s -> addPath(newVariable, s))
                        .map(s -> addPosition(newVariable, s))
                        .map(s -> addSlide(newVariable, s))
                        .single();
                opcUaVariables.add(newVariable);
            }
        }
        return variablesMatcher.replaceFirst(variableRegex);
    }

    private String addNode(OpcUaVariable variable, String content)
    {
        //Find variable type.
        String nodeRegex = NODE_PATTERN_OPENING + ANY_SIGN_PATTERN + NODE_PATTERN_CLOSING;
        Pattern nodePattern = Pattern.compile(nodeRegex);
        Matcher nodeMatcher = nodePattern.matcher(content);
        if(nodeMatcher.find())
        {
            variable.setNode(Integer.parseInt(nodeMatcher.group()
                    .replace(NODE_PATTERN_OPENING,"")
                    .replace(NODE_PATTERN_CLOSING, "")));

        }
        return nodeMatcher.replaceFirst(nodeRegex);
    }

    private String addPath(OpcUaVariable variable, String content)
    {
        //Find variable type.
        String pathRegex = PATH_PATTERN_OPENING + ANY_SIGN_PATTERN + PATH_PATTERN_CLOSING;
        Pattern pathPattern = Pattern.compile(pathRegex);
        Matcher pathMatcher = pathPattern.matcher(content);
        if(pathMatcher.find())
        {
            variable.setPath(pathMatcher.group()
                    .replace(PATH_PATTERN_OPENING,"")
                    .replace(PATH_PATTERN_CLOSING, ""));
        }
        return pathMatcher.replaceFirst(pathRegex);
    }

    private String addPosition(OpcUaVariable variable, String content)
    {
        //Find screen position.
        float x = 0;
        float y = 0;
        String xRegex = X_POSITION_PATTERN_OPENING + ANY_SIGN_PATTERN + X_POSITION_PATTERN_CLOSING;
        Pattern xPattern = Pattern.compile(xRegex);
        Matcher xMatcher = xPattern.matcher(content);
        if(xMatcher.find())
        {
            x = Float.parseFloat(xMatcher.group()
                    .replace(X_POSITION_PATTERN_OPENING,"")
                    .replace(X_POSITION_PATTERN_CLOSING, ""));
        }
        String temp =  xMatcher.replaceFirst(xRegex);

        String yRegex = Y_POSITION_PATTERN_OPENING + ANY_SIGN_PATTERN + Y_POSITION_PATTERN_CLOSING;
        Pattern yPattern = Pattern.compile(yRegex);
        Matcher yMatcher = yPattern.matcher(temp);
        if(yMatcher.find())
        {
            y = Float.parseFloat(yMatcher.group()
                    .replace(Y_POSITION_PATTERN_OPENING,"")
                    .replace(Y_POSITION_PATTERN_CLOSING, ""));
        }
        //Add position.
        variable.setScreenPosition(x,y);

        return yMatcher.replaceFirst(yRegex);
    }

    private String addSlide(OpcUaVariable variable, String content)
    {
        //Find variable type.
        String slideRegex = SLIDE_PATTERN_OPENING + ANY_SIGN_PATTERN + SLIDE_PATTERN_CLOSING;
        Pattern slidePattern = Pattern.compile(slideRegex);
        Matcher slideMatcher = slidePattern.matcher(content);
        if(slideMatcher.find())
        {
            variable.setSlideNumber(Integer.parseInt(slideMatcher.group()
                    .replace(SLIDE_PATTERN_OPENING,"")
                    .replace(SLIDE_PATTERN_CLOSING, "")));
        }
        return slideMatcher.replaceFirst(slideRegex);
    }
    private String addBounds(String content) {
        String minInString = "";
        String maxInString = "";

        String minRegex = MIN_BOUNDS_PATTERN_OPENING + ANY_SIGN_PATTERN + MIN_BOUNDS_PATTERN_CLOSING;
        String maxRegex = MAX_BOUNDS_PATTERN_OPENING + ANY_SIGN_PATTERN + MAX_BOUNDS_PATTERN_CLOSING;

        Pattern minPattern = Pattern.compile(minRegex);
        Pattern maxPattern = Pattern.compile(maxRegex);

        Matcher minMatcher = minPattern.matcher(content);
        Matcher maxMatcher = maxPattern.matcher(content);

        if(minMatcher.find() && maxMatcher.find())
        {
            minInString = minMatcher.group()
                    .replace(MIN_BOUNDS_PATTERN_OPENING,"")
                    .replace(MIN_BOUNDS_PATTERN_CLOSING, "");
            maxInString = maxMatcher.group()
                    .replace(MAX_BOUNDS_PATTERN_OPENING,"")
                    .replace(MAX_BOUNDS_PATTERN_CLOSING, "");

            //Create bounds list.
            Float[] minInFloats = Stream.of(minInString.split(","))
                    .map(s -> Float.parseFloat(s))
                    .toArray(Float[]::new);

            Float[] maxInFloats = Stream.of(maxInString.split(","))
                    .map(s -> Float.parseFloat(s))
                    .toArray(Float[]::new);

            //Connect floats to MeasureBounds object lists.
            for(int i = 0; i < minInFloats.length && i < maxInFloats.length; ++i)
            {
                measureBoundsList.add(new MeasureBounds(minInFloats[i], maxInFloats[i]));
            }

        }

        return content.replace(minInString,"").replace(maxInString,"");
    }

    private void getMeasuresBoundsFromFile() throws IOException
    {

    }

    public String getProgramName()
    {
        return programName;
    }


    /**
     * Method to get program path.
     */
    public void setProgramPath(String path)
    {
        this.programPath = path;
    }

    /**
     * Method to set program path.
     */
    public String getProgramPath()
    {
        return this.programPath;
    }

    /**
     * Get access to previous picture.
     * @throws IOException
     */
    public InputStream getPreviousPicture() throws Exception
    {
            if(pictureNumber == 0)
                throw new Exception();
        try {
            return zipFile.getInputStream(entriesList.get(--pictureNumber));
        }catch (Exception e)
        {
            throw new Exception();
        }
    }

    public InputStream getNextPicture() throws Exception
    {
            if(pictureNumber == entriesList.size()-1 - ADDITIONAL_FILES_NUMBER)
                throw new Exception();
        try {
            return zipFile.getInputStream(entriesList.get(++pictureNumber));
        }catch (Exception e)
        {
            throw new Exception();
        }
    }

    public List<MeasureBounds> getMeasuresBounds()
    {
        return measureBoundsList;
    }

    public void reset()
    {
        pictureNumber = -1;
    }

    public String getAdditionalData(String panelName) throws Exception
    {
        return additionalInfo.getContent(pictureNumber, panelName);
    }

    public MeasureBounds getActualMeasureBounds()
    {
        return measureBoundsList.get(pictureNumber);
    }

    public MeasureBounds getBoundsForPoint(int point)
    {
        if(point < measureBoundsList.size())
            return measureBoundsList.get(point);
        else
            return null;
    }

    public InputStream getStlObject() throws IOException {
        return zipFile.getInputStream(zipFile.getEntry("stl_object"));
    }

}
