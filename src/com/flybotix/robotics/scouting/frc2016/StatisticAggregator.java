package com.flybotix.robotics.scouting.frc2016;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.plnyyanks.tba.apiv2.APIv2Helper;
import com.plnyyanks.tba.apiv2.interfaces.APIv2;
import com.plnyyanks.tba.apiv2.models.Event;
import com.plnyyanks.tba.apiv2.models.Match;

public class StatisticAggregator {
  private enum EDefenseCategory {
    A, B, C, D, LOW
  }
  
  public static final int sDEFENSE_STRENGTH = 2;
  
  private enum EDefense {
    LOW_BAR(EDefenseCategory.LOW,""),
    CDF(EDefenseCategory.A,"A_ChevalDeFrise"),
    PortC(EDefenseCategory.A, "A_Portcullis"),
    Moat(EDefenseCategory.B,"B_Moat"),
    Ramp(EDefenseCategory.B,"B_Ramparts"),
    DB(EDefenseCategory.C,"C_Drawbridge"),
    SP(EDefenseCategory.C, "C_SallyPort"),
    RW(EDefenseCategory.D, "D_RockWall"),
    RT(EDefenseCategory.D, "D_RoughTerrain");
    
    public final EDefenseCategory mCategory;
    public final String mTBAName;
    
    private EDefense(EDefenseCategory pCategory, String pTBAName) {
      mCategory = pCategory;
      mTBAName = pTBAName;
    }
  }
  
  public static EDefense getByTbaName(String pTbaDefenseName) {
    EDefense result = EDefense.LOW_BAR;
    for(EDefense defense : EDefense.values()) {
      if(pTbaDefenseName.equalsIgnoreCase(defense.mTBAName)) {
        result = defense;
        break;
      }
    }
    return result;
  }
  
  private class DefenseData {
  }
  
  private enum EScore {
    teleopPoints,
    robot3Auto,
    autoPoints,
    teleopScalePoints,
    autoBouldersLow,
    teleopTowerCaptured,
    teleopBouldersLow,
    teleopCrossingPoints,
    foulCount,
    foulPoints,
    techFoulCount,
    totalPoints,
    adjustPoints,
    position3,
    robot1Auto,
    position4,
    position5,
    autoBoulderPoints,
    teleopBoulderPoints,
    teleopBouldersHigh,
    autoBouldersHigh,
    robot2Auto,
    position1crossings,
    towerEndStrength,
    position2crossings,
    position3crossings,
    position4crossings,
    position5crossings,
    teleopChallengePoints,
    autoCrossingPoints,
    teleopDefensesBreached,
    autoReachPoints,
    position2,
    capturePoints,
    towerFaceB,
    towerFaceC,
    towerFaceA,
    breachPoints,
    ;
  }

  private static class Cross {
    public final EScore position;
    public final EScore crossing;
    public Cross(EScore pPosition, EScore pCrossing) {
      position = pPosition;
      crossing = pCrossing;
    }
  }
  
  private static final Cross[] sCROSSES = new Cross[] {
    new Cross(EScore.position2, EScore.position2crossings),
    new Cross(EScore.position3, EScore.position3crossings),
    new Cross(EScore.position4, EScore.position4crossings),
    new Cross(EScore.position5, EScore.position5crossings)
  };
  
  
  private enum EAlliance {
    red,
    blue;
  }
  
  public static final Set<MatchScores> sALL_MATCHES = new HashSet<>();
  
  private static class MatchScores {
    private final Map<EAlliance, List<String>> mAlliances = new HashMap<>();
    private final Map<EAlliance, MatchScoreBreakdown2016> mBreakdown = new HashMap<>();
    
    public MatchScores(JsonElement pAlliances, JsonElement pMatchBreakdown) {
      for(EAlliance alliance : EAlliance.values()) {
        mBreakdown.put(alliance, MatchScoreBreakdown2016.fromJSON(pMatchBreakdown.getAsJsonObject().get(alliance.name())));
        List<String> teams = new ArrayList<>();
        JsonElement element = pAlliances.getAsJsonObject().get(alliance.name()).getAsJsonObject().get("teams");
        element.getAsJsonArray().forEach(team -> teams.add(team.getAsString().replace("frc", "")));
        mAlliances.put(alliance, teams);
      }
    }
  }
  
  private static class MatchScoreBreakdown2016 {
    public final Map<EScore, String> mScore = new EnumMap<>(EScore.class);
    public static MatchScoreBreakdown2016 fromJSON(JsonElement pJson) {
      MatchScoreBreakdown2016 score = new MatchScoreBreakdown2016();
      JsonObject obj = pJson.getAsJsonObject();
      for(EScore escore : EScore.values()) {
        score.mScore.put(escore, obj.get(escore.name()).getAsString());
      }
      return score;
    }
  }
  
  private static final Map<String, TeamData> sALL_TEAMS = new HashMap<>();
  
  private static class TeamData {
    private final String mNumber;
    private final Map<EDefense, Integer> mCrosses = new HashMap<>();
    private final Map<EDefense, Integer> mOpportunities = new HashMap<>();
    private final Set<MatchScoreBreakdown2016> mMatches = new HashSet<>();
    
    public TeamData(String pNumber) {
      for(EDefense d : EDefense.values()) {
        mCrosses.put(d, new Integer(0));
      }
      for(EDefense d : EDefense.values()) {
        mOpportunities.put(d, new Integer(0));
      }
      mNumber = pNumber;
    }
    
    public static String getTitleCSV() {
      StringBuilder sb = new StringBuilder();
      sb.append("Team").append(',');
      for(int i = 0 ; i < EDefense.values().length; i++) {
        EDefense d = EDefense.values()[i];
        sb.append(d.name() + " Oppy").append(',');
        sb.append(d.name() + " Cross").append(',');
        sb.append(d.name() + '%').append(',');
      }
      sb.append("# Matches");
      return sb.toString();
    }
    
    public String getDefenseCSV() {
      StringBuilder sb = new StringBuilder();
      sb.append(mNumber).append(',');
      for(int i = 0 ; i < EDefense.values().length; i++) {
        EDefense d = EDefense.values()[i];
        sb.append(mOpportunities.get(d)).append(',');
        sb.append(mCrosses.get(d)).append(',');
        sb.append((float)mCrosses.get(d) / (float)mOpportunities.get(d)).append(',');
      }
      sb.append(mMatches.size());
      return sb.toString();
    }
    
    public void addOppy(EDefense pDefense) {
      mOpportunities.put(pDefense, ((int)mOpportunities.get(pDefense) + 1));
    }
    
    public void addCross(EDefense pDefense) {
      mCrosses.put(pDefense, ((int)mCrosses.get(pDefense) + 1));
    }
    
    public void addMatch(MatchScoreBreakdown2016 pScore) {
      for(int i = 0 ; i < sDEFENSE_STRENGTH; i++) {
        addOppy(EDefense.LOW_BAR);
      }
      mMatches.add(pScore);
    }
  }
  
  public static TeamData getTeam(String pTeam) {
    TeamData result = sALL_TEAMS.get(pTeam);
    if(result == null) {
      result = new TeamData(pTeam);
      sALL_TEAMS.put(pTeam, result);
    }
    return result;
  }
  
  public static void main(String[] pArgs) {
    APIv2Helper.setAppId("JesseK:apitest:v1.0");
    APIv2 api = APIv2Helper.getAPI();
    List<Event> chsEvents = api.fetchDistrictEvents("chs", 2016, null);
    for(Event event : chsEvents) {
      String key = event.getKey();
      List<Match> matches = api.fetchEventMatches(key, null);
      if(matches != null) {
        for(Match m : matches) {
          if(m != null) {
            try {
              MatchScores score = new MatchScores(m.getAlliances(), m.getScore_breakdown());
              sALL_MATCHES.add(score);
            } catch (Exception e) {
              System.err.println(m.getKey() + "\t" + e.getMessage());
            }
          }
        }
      }
    }
    
    // Aggregate data by team
    for(MatchScores score : sALL_MATCHES) {
      for(EAlliance alliance : EAlliance.values()) {
        for(String team : score.mAlliances.get(alliance)) {
          getTeam(team).addMatch(score.mBreakdown.get(alliance));
        }
      }
    }
    
    System.out.println(TeamData.getTitleCSV());
    // Now each team should have a set of matches for the entire season, so we start counting
    for(String team : sALL_TEAMS.keySet()) {
      TeamData data = sALL_TEAMS.get(team);
      data.mMatches.forEach((match) -> {
        for(int i = 0; i < Integer.parseInt(match.mScore.get(EScore.position1crossings)); i++) {
          data.addCross(EDefense.LOW_BAR);
        }
        
        for(Cross cross : sCROSSES) {
          EDefense pos = getByTbaName(match.mScore.get(cross.position));
          for(int i = 0; i < sDEFENSE_STRENGTH; i++) {
            data.addOppy(pos);
          }
          for(int i = 0; i < Integer.parseInt(match.mScore.get(cross.crossing)); i++) {
            data.addCross(pos);
          }
        }
      });
      
      System.out.println(data.getDefenseCSV());
    }
  }
}

