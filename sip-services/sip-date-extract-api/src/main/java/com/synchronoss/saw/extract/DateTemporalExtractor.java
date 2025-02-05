package com.synchronoss.saw.extract;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import com.synchronoss.saw.extract.traversal.ExtractHelper;
import com.synchronoss.saw.extract.traversal.TemporalPredictTraversalNode;
import com.synchronoss.saw.extract.traversal.TrieTree;
import com.synchronoss.saw.store.base.BaseStructure;
import com.synchronoss.saw.store.base.TemporalDateStructure;
import org.apache.commons.lang3.time.DateUtils;

public class DateTemporalExtractor {
  String delim = "-.\\/|:, ";
  String learnedPatternString = null;
  boolean learnPattern = false;

  /**
   * Provides the {@link List} of {@link TemporalDateStructure} as output
   * 
   * @param text
   *            source text
   * @return
   */
  public List<TemporalDateStructure> parse(String text) {
      List<TemporalDateStructure> dateGroups = new ArrayList<TemporalDateStructure>();
      List<BaseStructure> groups = getDateGroups(text);
      // System.out.println(groups);
      if (groups == null) {
          return dateGroups;
      }
      for (BaseStructure element : groups) {
          TemporalDateStructure localdate = getDateFromPhrase(element);
          if (localdate != null) {
              localdate.setStart(element.getStartPos());
              localdate.setEnd(element.getEndPos());
              if (element.getTimeFragment() != null) {
                  localdate = putTimeInDate(localdate, element);
              }
              String delims = element.getDateFragment().replaceAll("[A-Za-z0-9]", "");
              String foundFormat = localdate.getIdentifiedDateFormat();
              if(delims.length()==2) {
                  foundFormat=foundFormat.replaceAll("\\$", String.valueOf(delims.charAt(0)));
                  foundFormat=foundFormat.replaceAll("&", String.valueOf(delims.charAt(1)));
              }
              if(element.isIsAlphaNumeric() && ExtractHelper.isFullMonth(element.getData())) {
                  foundFormat = foundFormat.replaceAll("MMM", "MMMM");
              }
              localdate.setIdentifiedDateFormat(foundFormat);
              if(isCorrectFormat(text,foundFormat)){
                  dateGroups.add(localdate);
              }
          }
      }
      return dateGroups;
  }

    public boolean isCorrectFormat(String dateString, String... dateFormat)  {
        try {
            DateUtils.parseDateStrictly(dateString, dateFormat);
            return true;
        }catch(ParseException exception){
            return false;
        }
    }

  /**
   *
   * @param element
   * @return
   */
  private TemporalDateStructure getDateFromPhrase(BaseStructure element) {
      boolean notFound = true;
      String s = null;
      String presentDateFormat="";
      if (element.getTimeFragment() == null) {
          s = element.getData();
      } else {
          s = element.getDateFragment();
      }
      if (element.isIsAlphaNumeric()) {
          StringTokenizer tokenizer = new StringTokenizer(s, delim);
          String t1 = tokenizer.nextToken();
          String t2 = tokenizer.nextToken();
          String t3 = tokenizer.nextToken();

          int day;
          int month;
          int year;

          // scenario detemisnistic where t1>31 & t1< 9999
          // yyyy MM dd
          // yyyy dd MM
          // yy dd MM
          // yy MM dd
          // in case of yy (MM/dd) if yy<32, date can not be detrmined
          if (ExtractHelper.isDigit(t1)) {
              year = Integer.parseInt(t1);
              if (year > 31) {
                  /*determine the format of year to find out present date format*/
                  if(year < 99) {
                      presentDateFormat ="yy$";
                  }
                  else if(year > 999 && year < 10000) {
                      presentDateFormat ="yyyy$";
                  }
                  if (ExtractHelper.isDigit(t2)) {
                      presentDateFormat = presentDateFormat +"dd&MMM";
                      day = Integer.parseInt(t2);
                      month = monthToDigit(t3);
                  } else {
                      presentDateFormat = presentDateFormat +"MMM&dd";
                      month = monthToDigit(t2);
                      day = Integer.parseInt(t3);
                  }
                  TemporalDateStructure localDate = getYyyyMmDdProbable(year, month, day);
                  if (localDate == null) {
                      notFound = true;
                  } else {
                      localDate.setOriginalText(element.getData());
                      localDate.setIdentifiedDateFormat(presentDateFormat);
                  }
                  return localDate;
              }
          }

          // scenario detemisnistic where t1>31 & t1< 9999
          // dd MM yyyy
          // dd MM yy
          // MM dd yyyy
          // MM dd yy
          // in case of (MM/dd) yy if yy<32, date can not be detrmined
          if (ExtractHelper.isDigit(t3)) {
              year = Integer.parseInt(t3);
              if (year > 31) {
                  /*determine the format of year to find out present date format*/
                  if(year < 99) {
                      presentDateFormat ="yy";
                  }
                  else if(year > 999 && year < 10000) {
                      presentDateFormat ="yyyy";
                  }
                  if (ExtractHelper.isDigit(t1)) {
                      presentDateFormat = "dd$MMM&" + presentDateFormat;
                      day = Integer.parseInt(t1);
                      month = monthToDigit(t2);
                  } else {
                      presentDateFormat = "MMM$dd&" + presentDateFormat;
                      month = monthToDigit(t1);
                      day = Integer.parseInt(t2);
                  }
                  TemporalDateStructure localDate = getYyyyMmDdProbable(year, month, day);
                  if (localDate == null) {
                      notFound = true;
                  } else {
                      localDate.setOriginalText(element.getData());
                      localDate.setIdentifiedDateFormat(presentDateFormat);
                  }
                  return localDate;
              }
          }

      } else {
          StringTokenizer tokenizer = new StringTokenizer(s, delim);
          int d1 = Integer.parseInt(tokenizer.nextToken());
          int d2 = Integer.parseInt(tokenizer.nextToken());
          int d3 = Integer.parseInt(tokenizer.nextToken());
          // supported formats
          // yyyy MM dd
          // yy MM dd
          // dd MM yyyy
          // dd MM yy
          // MM dd yyyy
          // MM dd yy
          // if d1 = year then d2 = month & d3 = day given d1>999 or 4 digit,
          // detrmining yyyy MM dd
          if (d1 > 999) {
              TemporalDateStructure localDate = getYyyyMmDdProbable(d1, d2, d3);
              if (localDate != null) {
                  localDate.setOriginalText(s);
                  if (learnPattern && learnedPatternString != null) {
                      learnedPatternString = localDate.getConDateFormat();
                  }
                  notFound = false;
                  return localDate;
              }
          }
          // determined that 2 digit date is not a month or day as its between
          // 31 & 99 , deteremining yy MM dd
          if (notFound && d1 > 31 && d1 < 100) {
              TemporalDateStructure localDate = getYyMmDdProbable(d1, d2, d3);
              if (localDate != null) {
                  localDate.setOriginalText(s);
                  if (learnPattern && learnedPatternString != null) {
                      learnedPatternString = localDate.getConDateFormat();
                  }
                  notFound = false;
                  return localDate;
              }
          }
          // if d3= year in yyyy format, then date and month not determinstic
          // if d1 & d2 <= 12
          // determine in case one position is dterministic
          //TODO
          if (d3 > 999 && ((d1 > 0 && d1 < 32) || (d2 > 0 && d2 < 32)) && (d1 > 0 && d2 > 0)) {
              TemporalDateStructure localDate = getDetemintaionForYyyyPrefix(d1, d2, d3);
              if (localDate != null) {
                  localDate.setOriginalText(s);
                  if (learnPattern && learnedPatternString != null) {
                      learnedPatternString = localDate.getConDateFormat();
                  }
                  notFound = false;
                  return localDate;
              }
          }
          // if d3=year in yy format i.e. d3>31 & d3< 100, possible date with
          // month and day not determinsitic
          //TODO
          if ((d3 > 31 && d3 < 100) && ((d1 > 0 && d1 < 32) || (d2 > 0 && d2 < 32))) {
              TemporalDateStructure localDate = getDetemintaionForYyyyPrefix(d1, d2, d3);
              if (localDate != null) {
                  localDate.setOriginalText(s);
                  if (learnPattern && learnedPatternString != null) {
                      learnedPatternString = localDate.getConDateFormat();
                  }
                  notFound = false;
                  return localDate;
              }
          }
      }
      return null;
  }

  private TemporalDateStructure putTimeInDate(TemporalDateStructure localdate, BaseStructure element) {
      // supported time format is HH(24):MI:SS.SSS (AM/PM)
      String format = "HH:mm:ss";
      String s = element.getTimeFragment();
      int hour;
      int min;
      int sec;
      int mils = 0;
      String probableTimeFormat=format;

      StringTokenizer tokenizer = null;
      // time has miliseconds in it
      if (s.contains(".") || s.contains(",")) {
          tokenizer = new StringTokenizer(s, ":., ");
          hour = Integer.parseInt(tokenizer.nextToken());
          min = Integer.parseInt(tokenizer.nextToken());
          sec = Integer.parseInt(tokenizer.nextToken());
          mils = Integer.parseInt(tokenizer.nextToken());
          //format = format + ".SSS";
          if(s.contains(".")) {
              probableTimeFormat = format + ".SSS";   
          }
          if(s.contains(",")) {
              probableTimeFormat = format + ",SSS";   
          }
      } else {
          tokenizer = new StringTokenizer(s, ": ");
          hour = Integer.parseInt(tokenizer.nextToken());
          min = Integer.parseInt(tokenizer.nextToken());
          sec = Integer.parseInt(tokenizer.nextToken());
      }

      if (element.isHasAmPm()) {
          if (s.contains("pm")) {
              hour = hour + 12;
              probableTimeFormat = probableTimeFormat+" aaa";
          }
      }
      if (hour < 24 && min < 60 && sec < 60 && mils < 10000) {
          String timePiece;
          timePiece = String.format("%02d", hour) + ":" + String.format("%02d", min) + ":" + String.format("%02d", sec);
          if (mils >= 0 && (s.contains(".") || s.contains(","))) {
              timePiece = timePiece + "." + String.format("%03d", mils);
          }
          format = localdate.getConDateFormat() + " " + format;
          localdate.setConDateFormat(format);
          timePiece = localdate.getDateTimeString() + " " + timePiece;
          localdate.setDateTimeString(timePiece);
          String sep = (element.getDateTimeSeprator() == 'T')?"'T'":String.valueOf(element.getDateTimeSeprator());
          localdate.setIdentifiedDateFormat(localdate.getIdentifiedDateFormat()+sep+probableTimeFormat);
      }
      return localdate;
  }

  public TemporalDateStructure getDetemintaionForYyPrefix(int d1, int d2, int pYear) {
      TemporalDateStructure localDate = getDetemintaionForYyyyPrefix(d1, d2, pYear);
      if (localDate != null) {
          /*
           * String format = localDate.getConDateFormat().replace("yyyy",
           * "yy"); localDate.setConDateFormat(format);
           */
      }
      return localDate;
  }

  private TemporalDateStructure getDetemintaionForYyyyPrefix(int d1, int d2, int pYear) {
      // if d1 is between 13 and 31, it can be day, depending its valid day of
      // month d2
      if (d1 > 12 && (d2>0 && d2<=12)) {
          int pDate = d1;
          int pMonth = d2;
          TemporalDateStructure localDate = getYyyyMmDdProbable(pYear, pMonth, pDate);
          if (localDate != null) {
              // localDate.setConDateFormat("dd-MM-yyyy");
              if(pYear > 9 & pYear < 100) {
                  localDate.setIdentifiedDateFormat("dd$MM&yy");
              }
              if(pYear > 99 & pYear < 10000) {
                  localDate.setIdentifiedDateFormat("dd$MM&yyyy");
              }
              return localDate;
          }
      }
      if (d2 > 12 && (d1>0 && d1<=12)) {
          int pDate = d2;
          int pMonth = d1;
          TemporalDateStructure localDate = getYyyyMmDdProbable(pYear, pMonth, pDate);
          if (localDate != null) {
              // localDate.setConDateFormat("MM-dd-yyyy");
              if(pYear > 9 & pYear < 100) {
                  localDate.setIdentifiedDateFormat("MM$dd&yy");
              }
              if(pYear > 99 & pYear < 10000) {
                  localDate.setIdentifiedDateFormat("MM$dd&yyyy");
              }
              return localDate;
          }
      }
      if(d1 <= 12 && d2 <= 12) {
          // Assumption is MM dd, i.e. Month will always be d2 and date will be d3
          int pMonth=d1;
          int pDate=d2;
          TemporalDateStructure localDate = getYyyyMmDdProbable(pYear, pMonth, pDate);
          if (localDate != null) {
              // localDate.setConDateFormat("MM-dd-yyyy");
              if(pYear > 9 & pYear < 100) {
                  localDate.setIdentifiedDateFormat("MM$dd&yy");
              }
              if(pYear > 99 & pYear < 10000) {
                  localDate.setIdentifiedDateFormat("MM$dd&yyyy");
              }
              return localDate;
          }
      }
      return null;
  }

  private TemporalDateStructure getYyMmDdProbable(int pYear, int pMonth, int pDay) {
      TemporalDateStructure localDate = getYyyyMmDdProbable(pYear, pMonth, pDay);
      if (localDate != null) {
          // localDate.setConDateFormat("yy-MM-dd");
      }
      return localDate;
  }

  private TemporalDateStructure getYyyyMmDdProbable(int pYear, int pMonth, int pDay) {
      boolean isDateProbable = true;
      String formatProbable="";
      int year = -100;
      int month = -100;
      int day = -100;
      // Pobabability month.max=12,day.max=12
      year = pYear;
      // month should be between 1 & 12, else its not a date
      if (pMonth > 0 && pMonth < 13) {
          month = pMonth;
          formatProbable = "MM&dd";
      }else if(pMonth > 12 && pDay < 13) {
          month=pDay;
          pDay = pMonth;
          formatProbable = "dd&MM";
      }else {
          return null;
      }
      // day should be between 1 & 31 depending on months
      if (isDateProbable && month == 2) {
          if (year % 4 > 0 && pDay < 29) {
              // in feburary day.max=28 given year is non leap
              day = pDay;
          } else if (year % 4 == 0 && pDay < 30) {
              // in feburary day.max=28 given year is leap year
              day = pDay;
          } else {
              return null;
          }
      }
      // if month=1,3,5,7,8,10,12 day.max=31
      if (isDateProbable && is31DayMonth(month)) {
          if (pDay < 32) {
              day = pDay;
          } else {
              return null;
          }
      }
      // if month=4,6,9,11 day.max=30
      if (isDateProbable && is30DayMonth(month)) {
          if (pDay < 31) {
              day = pDay;
          } else {
              return null;
          }
      }
      if (isDateProbable) {
          TemporalDateStructure localdate = new TemporalDateStructure();
          localdate.setDateTimeString(String.format("%04d", year) + "-" + String.format("%02d", month) + "-" + String.format("%02d", day));
          localdate.setConDateFormat("yyyy-MM-dd");
          if(year > 9 & year < 100 ) {
              formatProbable = "yy$" + formatProbable;
          }
          if(year > 999 & year < 10000){
              formatProbable = "yyyy$" + formatProbable;
          }
          localdate.setIdentifiedDateFormat(formatProbable);
          return localdate;
      }
      return null;
  }

  /**
   * Parses source text to find date patterns
   * 
   * @param text
   *            source text
   * @return
   */
  private List<BaseStructure> getDateGroups(String text) {
      List<BaseStructure> dateGroups = null;
      char possibleDate[] = new char[29];
      char possibleTime[] = new char[17];
      int i = 0;
      boolean endFoundEarlier = false;
      boolean monthDetermined = false;
      boolean searchForTimePiece = false;
      int timeFrgLength = 0;
      char marker = '0';
      boolean isAlpaNumeric = false;
      char dateTimeSeprator=32;
      /*
       * to account number of whitespaces for determining start and end
       * position of date fragment, as multiple spaces are ignored
       */
      int whitespaceCount = 0;
      TemporalPredictTraversalNode tree = TrieTree.patternPredictionTree;
      TemporalPredictTraversalNode month = TrieTree.monthPredictionTree;
      TemporalPredictTraversalNode time = TrieTree.timePredictionTree;
      int textLength = text.length();
      for (int count = 0; count < textLength; count++) {

          char c = text.charAt(count);
          
          if (i == 0) {
              tree = TrieTree.patternPredictionTree;
          }
          
          // check for whitespace series
          if ((i > 1 && possibleDate[i - 1] == 32 && c == 32)) {
              whitespaceCount++;
              continue;
          }
          if (searchForTimePiece) {
              boolean timeDetermined = time.explictDateFragment;
              // for the cases where date is detemined just to establish am/pm
              // prefix
              if (timeFrgLength > 12 && possibleTime[timeFrgLength - 1] == 32) {
                  timeDetermined = true;
              }
              if (ExtractHelper.isDigit(c)) {
                  time = time.getChild('D');
              } else {
                  c = Character.toLowerCase(c);
                  time = time.getChild(c);
              }
              if (time == null) {
                  if (timeDetermined) {
                      dateGroups = addTimeFragment(dateGroups, possibleDate, possibleTime, count, i, timeFrgLength, dateTimeSeprator);
                  }/*if added as bug fix on date 12/15/2017*/
                  if(isValidTimeFragmentWithEndingDelim(possibleTime)) {
                      possibleTime[timeFrgLength -1] = ' ';
                      possibleDate[i -1] = ' ';
                      dateGroups = addTimeFragment(dateGroups, possibleDate, possibleTime, count, i - 1, timeFrgLength, dateTimeSeprator);
                  }
                  searchForTimePiece = false;
                  possibleDate = nullifyBuffer(possibleDate);
                  possibleTime = nullifyBuffer(possibleTime);
                  endFoundEarlier = false;
                  time = TrieTree.timePredictionTree;
                  i = 0;
                  whitespaceCount = 0;
                  timeFrgLength = 0;
                  continue;
              } else {
                  if (timeDetermined) {
                      if (count == text.length() - 1 || c == 32) {
                          dateGroups = addTimeFragment(dateGroups, possibleDate, possibleTime, count, i, timeFrgLength, dateTimeSeprator);
                          searchForTimePiece = false;
                          possibleDate = nullifyBuffer(possibleDate);
                          possibleTime = nullifyBuffer(possibleTime);
                          endFoundEarlier = false;
                          time = TrieTree.timePredictionTree;
                          i = 0;
                          whitespaceCount = 0;
                          timeFrgLength = 0;
                          continue;
                      }/* bug fixed dated 12/15/2017*/
                  }else {
                      if(count == text.length() - 1 && time.explictDateFragment == true) {
                          if(ExtractHelper.isDigit(c)) {
                              possibleTime[timeFrgLength++] = c;
                              possibleDate[i++] = c;
                          }
                          dateGroups = addTimeFragment(dateGroups, possibleDate, possibleTime, count, i, timeFrgLength, dateTimeSeprator);
                          searchForTimePiece = false;
                          possibleDate = nullifyBuffer(possibleDate);
                          possibleTime = nullifyBuffer(possibleTime);
                          endFoundEarlier = false;
                          time = TrieTree.timePredictionTree;
                          i = 0;
                          whitespaceCount = 0;
                          timeFrgLength = 0;
                          continue;
                      }
                  }
                  possibleTime[timeFrgLength++] = c;
                  possibleDate[i++] = c;

              }
              continue;
          }
          /* if current character is Number of separator */
          if (ExtractHelper.isDigit(c) || ExtractHelper.isDelimeter(c) || ExtractHelper.isTimeSeprator(c)) {
              /*
               * if marker was previously set for finding month and current
               * char is delimiter or digit, then see if month is determined.
               * If yes keep looking for pattern to complete as Month is not
               * supporting as ending pattern in date
               */
              if (marker == 'M') {
                  /*
                   * if month is determined previously, carry on further
                   * search in this iteration
                   */
                  if (monthDetermined) {
                      isAlpaNumeric = true;
                      tree = tree.getChild(marker);
                      /*
                       * if month is determined, date pattern established and
                       * nothing else left in tree, jump to next iteration
                       */
                      if (tree == null && endFoundEarlier) {
                          continue;
                      }
                  }
                  /*
                   * Month cannot be ending pattern in supported date format.
                   * Reset the buffers & criteria for establishing date
                   */
                  else {
                      tree = TrieTree.patternPredictionTree;
                      possibleDate = nullifyBuffer(possibleDate);
                      i = 0;
                      whitespaceCount = 0;
                      endFoundEarlier = false;
                  }
                  /*
                   * Month is already determined or rejected, in both cases
                   * reset month prediction tree to start again
                   */
                  month = TrieTree.monthPredictionTree;
              }

              marker = ExtractHelper.isDigit(c) ? 'D' : '*';
              /* last iteration yielded valid date fragment */
              if (tree == null) {
                  if (endFoundEarlier) {
                      dateGroups = addDateFragment(dateGroups, possibleDate, count, timeFrgLength + i + whitespaceCount, isAlpaNumeric);
                      isAlpaNumeric = false;
                      if (ExtractHelper.isDigit(c)) {
                          time = time.getChild('D');
                          if (time != null) {
                              possibleTime[timeFrgLength++] = c;
                              searchForTimePiece = true;
                              possibleDate[i++] = c;
                          } else {
                              possibleDate = nullifyBuffer(possibleDate);
                              i = 0;
                              whitespaceCount = 0;
                              endFoundEarlier = false;
                          }
                      } else {
                          possibleDate = nullifyBuffer(possibleDate);
                          i = 0;
                          whitespaceCount = 0;
                          endFoundEarlier = false;
                      }
                      endFoundEarlier = false;
                      tree = TrieTree.patternPredictionTree;
                      month = TrieTree.monthPredictionTree;
                      continue;
                  }
                  /* last iteration did not yielded valid date fragment */
                  else {
                      possibleDate = nullifyBuffer(possibleDate);
                      i = 0;
                      whitespaceCount = 0;
                      endFoundEarlier = false;
                      tree = TrieTree.patternPredictionTree;
                      month = TrieTree.monthPredictionTree;
                      continue;
                  }
              }
              // redesigned flow
              tree = tree.getChild(marker);

              if (tree == null) {
                  if (endFoundEarlier) {
                      dateGroups = addDateFragment(dateGroups, possibleDate, count, timeFrgLength + i + whitespaceCount, isAlpaNumeric);
                      endFoundEarlier = false;
                      if (ExtractHelper.isTimeSeprator(c)) {
                          searchForTimePiece = true;
                          possibleDate[i++] = c;
                          dateTimeSeprator = c;
                      } else {
                          possibleDate = nullifyBuffer(possibleDate);
                          i = 0;
                          whitespaceCount = 0;
                      }
                      endFoundEarlier = false;
                      tree = TrieTree.patternPredictionTree;
                      month = TrieTree.monthPredictionTree;
                  }
                  continue;
              } else {
                  // redesigned flow
                  possibleDate[i++] = c;

                  endFoundEarlier = tree.explictDateFragment;
                  // end of text is reached and date is determined, so add the
                  // date in date group as loop is terminated afterwards.
                  if (count == text.length() - 1 && endFoundEarlier) {
                      dateGroups = addDateFragment(dateGroups, possibleDate, count, timeFrgLength + i + whitespaceCount, isAlpaNumeric);
                      break;
                  }
              }
          }
          /*
           * if current char is not a digit or delimiter, it should be checked
           * for Alphabet, for determination of month in alphabetical format
           */
          else {
              marker = 'M';
              /*
               * in case date occour first be sure tree is not null and
               * traverse tree with 'M' as root
               */
              /*if (i == 0) {
                  tree = TrieTree.patternPredictionTree;
              }*/
              c = Character.toLowerCase(text.charAt(count));
              month = month.getChild(c);
              if (month == null) {
                  /*
                   * if end of date fragment is determined do not nullify date
                   * fragment
                   */
                  if (endFoundEarlier) {
                      dateGroups = addDateFragment(dateGroups, possibleDate, count, timeFrgLength + i + whitespaceCount, isAlpaNumeric);
                      endFoundEarlier = false;
                  }
                  possibleDate = nullifyBuffer(possibleDate);
                  i = 0;
                  whitespaceCount = 0;
                  month = TrieTree.monthPredictionTree;
                  tree = TrieTree.patternPredictionTree;
                  monthDetermined = false;
                  endFoundEarlier = false;
                  continue;
              } else {
                  monthDetermined = month.explictDateFragment;
                  // redesigned flow
                  possibleDate[i++] = c;
              }
          }
      }
      return dateGroups;
  }
  
  /**
   * add the date fragment to dategroup with other details
   * 
   * @param dateGroups
   * @param possibleDate
   * @param count
   * @param i
   * @param isAlpaNumeric
   */
  private List<BaseStructure> addDateFragment(List<BaseStructure> dateGroups, char[] possibleDate, int count, int patternLength, boolean isAlpaNumeric) {
      if (dateGroups == null) {
          dateGroups = new ArrayList<BaseStructure>();
      }
      BaseStructure dateElement = createDateFragment(possibleDate, count, patternLength, isAlpaNumeric);
      if (dateElement != null) {
          dateGroups.add(dateElement);
      }
      return dateGroups;
  }

     /**
      * 
      * @param possibleTime
      * @return
      */
      private boolean isValidTimeFragmentWithEndingDelim(char[] possibleTime) {
          String s = new String(possibleTime).trim();
          String original = new String(possibleTime).trim();
          s = s.replaceAll("[0-9]", "");
          if(s.length() > 2 && s.length()<=3) {
              if(s.startsWith("::") && (original.endsWith(",") || original.endsWith("."))) {
                  return true;
              }
          }
          return false;
      }

  /**
   * 
   * @param dateGroups
   * @param possibleDate
   * @param possibleTime
   * @param count
   * @param i
   * @param timeFrgLength
   * @return
   */
  private List<BaseStructure> addTimeFragment(List<BaseStructure> dateGroups, char[] possibleDate, char[] possibleTime, int count, int i, int timeFrgLength, char dateTimeSeprator) {
      if (dateGroups == null) {
          return null;
      }
      BaseStructure ele = dateGroups.get(dateGroups.size() - 1);
      ele.setData(new String(possibleDate).trim());
      ele.setEndPos(count);
      //ele.setStartPos(count - i);
      ele.setTimeFragment(new String(possibleTime).trim());
      if (possibleTime[timeFrgLength - 1] == 'm') {
          ele.setHasAmPm(true);
      }
      ele.setDateTimeSeprator(dateTimeSeprator);
      return dateGroups;
  }

  /**
   *
   * @param buffer
   * @param position
   * @param length
   * @param alphaNumType
   * @return
   */
  private BaseStructure createDateFragment(char[] buffer, int position, int length, boolean isAlpaNumeric) {
      String dateText = new String(buffer).trim();
      if (dateText == null || "".equals(dateText)) {
          return null;
      }
      BaseStructure ele = new BaseStructure(new String(buffer).trim());
      /*
       * Decrease position by 1 as position in array position starts from '0'
       * and length is natural number
       */
      length--;
      ele.setEndPos(position);
      ele.setStartPos(position - length);
      ele.setIsAlphaNumeric(isAlpaNumeric);
      ele.setDateFragment(ele.getData());
      return ele;
  }

  /**
   *
   * @param buffer
   * @return
   */
  private char[] nullifyBuffer(char[] buffer) {
      for (int i = 0; i < buffer.length; i++) {
          buffer[i] = 0;
      }
      return buffer;
  }

  /**
   *
   * @param i
   * @return
   */
  private boolean is31DayMonth(int i) {
      if (i == 1) {
          return true;
      }
      if (i == 3) {
          return true;
      }
      if (i == 5) {
          return true;
      }
      if (i == 7) {
          return true;
      }
      if (i == 8) {
          return true;
      }
      if (i == 10) {
          return true;
      }
      if (i == 12) {
          return true;
      }
      return false;

  }

  /**
   *
   * @param i
   * @return
   */
  private boolean is30DayMonth(int i) {
      if (i == 4) {
          return true;
      }
      if (i == 6) {
          return true;
      }
      if (i == 9) {
          return true;
      }
      if (i == 11) {
          return true;
      }
      return false;
  }

  /**
   *
   * @param text
   * @return
   */
  private int monthToDigit(String text) {
      text = text.toLowerCase();
      if (text.equals("jan") || text.equals("january")) {
          return 1;
      }
      if (text.equals("feb") || text.equals("february")) {
          return 2;
      }
      if (text.equals("mar") || text.equals("march")) {
          return 3;
      }
      if (text.equals("apr") || text.equals("april")) {
          return 4;
      }
      if (text.equals("may")) {
          return 5;
      }
      if (text.equals("jun") || text.equals("june")) {
          return 6;
      }
      if (text.equals("jul") || text.equals("july")) {
          return 7;
      }
      if (text.equals("aug") || text.equals("august")) {
          return 8;
      }
      if (text.equals("sep") || text.equals("september")) {
          return 9;
      }
      if (text.equals("oct") || text.equals("october")) {
          return 10;
      }
      if (text.equals("nov") || text.equals("november")) {
          return 11;
      }
      if (text.equals("dec") || text.equals("december")) {
          return 12;
      }
      return -1;

  }
}
