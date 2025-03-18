function actionServer2Json(server) {
    let json = {};
    json["sid"] = server.getUid();
    json["name"] = server.getName();
    json["showName"] = server.getShowName();
    json["openTime"] = formatUTCTimestamp(server.getOpenTime());/*格式化日期*/
    json["ordinal"] = server.getOrdinal();/*格式化日期*/
    json["wlan"] = server.getWlan();
    json["port"] = server.getPort();
    return json;
}