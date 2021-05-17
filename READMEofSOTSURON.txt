##########################################################
#   2020年度　学部卒論実験プログラム the one simulator   #
##########################################################

プログラムファイル：
  C:\Users\SAAMIKA\eclipse-workspace\RBDR

①必要最低限の知識（ここだけいじれれば動く）
Setting.java：シナリオファイルを設定
  public static final String DEF_SETTINGS_FILE ="hogehoge.txt";
    ↑をシナリオファイル内の Scenario.name = hogehoge.txtに書き換え

DTNHost.java,Report.java：
  static変数 addressFromHigh と address FromAdjを書き換え
    →本来であれば、両方の変数を同一化し、かつ、シナリオファイルから設定可能にすべき。