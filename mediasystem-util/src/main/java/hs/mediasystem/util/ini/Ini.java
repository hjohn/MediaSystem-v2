package hs.mediasystem.util.ini;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Ini implements Iterable<List<Section>> {
  private final File iniFile;
  private final Map<String, List<Section>> sectionLists;

  public Ini(File iniFile) {
    this.iniFile = iniFile;

    if(iniFile.exists()) {
      try {
        sectionLists = readIniFile(iniFile);
      }
      catch(FileNotFoundException e) {
        throw new RuntimeException("Unable to load ini: " + iniFile.getAbsolutePath(), e);
      }
      catch(IOException e) {
        throw new RuntimeException(e);
      }
    }
    else {
      sectionLists = new HashMap<>();
    }
  }

  public Ini() {
    sectionLists = new HashMap<>();
    iniFile = null;
  }

  public boolean isEmpty() {
    return sectionLists.isEmpty();
  }

  public void addSection(Section section) {
    sectionLists.computeIfAbsent(section.getName(), k -> new ArrayList<>()).add(section);
  }

  public Section getSection(String sectionName) {
    List<Section> list = sectionLists.get(sectionName);

    if(list == null || list.isEmpty()) {
      return null;
    }

    if(list.size() > 1) {
      throw new IllegalArgumentException("Section '" + sectionName + "' is not unique");
    }

    return list.get(0);
  }

  public List<Section> getSections(String sectionName) {
    return sectionLists.getOrDefault(sectionName, Collections.emptyList());
  }

  public String getValue(String sectionName, String key) {
    Section section = getSection(sectionName);

    if(section != null) {
      return section.get(key);
    }

    return "";
  }

  @Override
  public Iterator<List<Section>> iterator() {
    return sectionLists.values().iterator();
  }

  public void save() {
    if(iniFile != null) {
      try {
        try(PrintWriter writer = new PrintWriter(new FileWriter(iniFile))) {
          boolean first = true;

          for(List<Section> sections : sectionLists.values()) {
            for(Section section : sections) {
              if(!first) {
                writer.println();
              }
              writer.println("[" + section.getName() + "]");
              first = false;

              for(String key : section) {
                for(String value : section.getAll(key)) {
                  writer.println(key + "=" + value);
                }
              }
            }
          }
        }
      }
      catch(IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private static final Pattern SECTION_PATTERN = Pattern.compile("\\[([-A-Za-z0-9\\.]+)\\](\\s*:\\s*\\[([-A-Za-z0-9\\.]+)\\])?");

  private static Map<String, List<Section>> readIniFile(File iniFile) throws IOException {
    try(BufferedReader reader = new BufferedReader(new FileReader(iniFile))) {
      Map<String, List<Section>> sectionLists = new LinkedHashMap<>();
      Section currentSection = null;

      for(;;) {
        String line = reader.readLine();

        if(line == null) {
          break;
        }

        line = line.trim();

        if(line.startsWith("#")) {
          continue;
        }

        Matcher matcher = SECTION_PATTERN.matcher(line);

        if(matcher.matches()) {
          String sectionName = matcher.group(1);

          Section parentSection = null;

          if(matcher.group(3) != null) {
            List<Section> parentSections = sectionLists.get(matcher.group(3));

            if(parentSections == null || parentSections.isEmpty()) {
              throw new RuntimeException("Parse Error, Parent '" + matcher.group(3) + "' not found for section '" + sectionName + "'");
            }
            if(parentSections.size() > 1) {
              throw new RuntimeException("The section used as parent for '" + sectionName + "' is not unique: " + matcher.group(3));
            }

            parentSection = parentSections.get(0);
          }

          currentSection = new Section(sectionName, parentSection);
          sectionLists.computeIfAbsent(currentSection.getName(), k -> new ArrayList<>()).add(currentSection);
        }
        else if(currentSection != null) {
          int eq = line.indexOf('=');

          if(eq > 0) {
            currentSection.put(line.substring(0, eq).trim(), line.substring(eq + 1).trim());
          }
        }
      }

      return sectionLists;
    }
  }
}
