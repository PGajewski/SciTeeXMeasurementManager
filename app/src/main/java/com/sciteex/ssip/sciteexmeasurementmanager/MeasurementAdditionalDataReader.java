package com.sciteex.ssip.sciteexmeasurementmanager;

import com.annimon.stream.Exceptional;
import com.annimon.stream.Stream;
import com.annimon.stream.function.BooleanConsumer;
import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Gajos on 9/27/2017.
 */

public class MeasurementAdditionalDataReader {

    //Patterns
    /**
     * Pattern of slide title (begin of slide)
     */
    private final static Pattern slidePattern = Pattern.compile("<slide>");
    private final static Pattern panelPattern = Pattern.compile("<panel>");
    private final static String titlePanelOpeningTag = "<panelTitle>";
    private final static String titlePanelClosingTag = "</panelTitle>";
    private final static String titleAvailableSigns = "[A-Za-z0-9]*";
    private final static Pattern panelTitlePattern = Pattern.compile(titlePanelOpeningTag + titleAvailableSigns + titlePanelClosingTag);

    public MeasurementAdditionalDataReader() {
        super();
    }

    private List<Slide> slides = new ArrayList<>();

    private String additionalInfoPath;

    public void setProgramPath(String programPath)
    {
        additionalInfoPath = programPath + ".info";
    }

    /**
     * Method to get data from additional file.
     */
    public void open()
    {
        try {
            String fileContent = new String(Files.toByteArray(new File(additionalInfoPath)));

            //Delete white signs.
            //fileContent = fileContent.replaceAll("\\s+","");

            //Split to slides
            String[] slideCodes = slidePattern.split(fileContent);
            for(String slideCode : Arrays.copyOfRange(slideCodes,1,slideCodes.length))
            {
                //Create new slide
                Slide slide = new Slide();
                String[] panels = panelPattern.split(slideCode);
                for(String panelCode : Arrays.copyOfRange(panels,1,panels.length))
                {
                    //Create new panel.
                    InfoPanel panel = new InfoPanel();

                    //Capture title.
                    try {
                        Matcher panelMatcher = panelTitlePattern.matcher(panelCode);
                        boolean isTitle = panelMatcher.find();
                        if(isTitle)
                        {

                            String title = panelMatcher.group();

                            //Replace tags.
                            title = title.replaceAll(titlePanelOpeningTag,"");
                            title = title.replaceAll(titlePanelClosingTag,"");
                            panel.setPanelName(title);

                        }

                        //Capture content.
                        panel.setContent(panelMatcher.replaceFirst(""));

                        //Add panel
                        slide.addPanel(panel);
                    }catch(Exception e)
                    {
                        //Ignore matcher exceptions.
                    }
                }

                //Add created slide to slide container.
                slides.add(slide);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public String getContent(int slideNumber, String panelName) throws Exception
    {
        return slides.get(slideNumber).getContent(panelName);
    }

    /**
     *  Inner class - container for data in panels.
     */
    public class InfoPanel
    {
        /**
         * String with content in html style.
         */
        private String content;

        /**
         * String with panel name
         */
        private String panelName;

        //Setters.
        public void setContent(String content)
        {
            this.content = content;
        }

        public void setPanelName(String panelName)
        {
            this.panelName = panelName;
        }

        //Getters.
        public String getContent()
        {
            return this.content;
        }

        public String getPanelName()
        {
            return this.panelName;
        }
    }

    public class Slide
    {
        /**
         * List for info panels.
         */
        List<InfoPanel> infoPanels = new ArrayList<>();

        /**
         * Title of slide
         */
        String slideTitle;

        //Setters.
        public void setSlideTitle(String slideTitle)
        {
            this.slideTitle = slideTitle;
        }

        //Getters.
        public String getSlideTitle()
        {
            return this.slideTitle;
        }

        /**
         * method to add panel to list.
         * @param panel
         */
        public void addPanel(InfoPanel panel)
        {
            infoPanels.add(panel);
        }

        public String getContent(String panelName) throws Exception
        {
            String content = Stream.of(infoPanels)
                    .filter(p->p.panelName.equals(panelName))
                    .findFirst()
                    .map(p -> p.getContent())
                    .get();
            if(content == null)
                throw new Exception("no_pannel");
            return content;
        }
    }

}
