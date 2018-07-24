package it.java.brs;

import it.common.AbstractIT;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Test;

public class AlternativeRealityTest extends AbstractIT {

  @Test
  public void normalReality() throws IOException, ParseException, InterruptedException {
    for(JSONObject jsonObject:getReality("reality1.json")) {
      super.processBlock(jsonObject);
      Thread.sleep(500);
    }
  }

  public List<JSONObject> getReality(String realityName) throws ParseException, IOException {
    JSONParser parser = new JSONParser();

    Path path = Paths.get("test/resources/alternatereality/" + realityName);

    String inputFileContent = new String(Files.readAllBytes(path));
    JSONArray array = (JSONArray) parser.parse(inputFileContent);

    List<JSONObject> result = new ArrayList<>();

    for(Object obj:array) {
      result.add((JSONObject) obj);
    }

    return result;
  }
}
