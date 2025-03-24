function sendRole(host, webPort, gameId, sid, roleId, title, content, goodsList) {
    let postJson = "";
    postJson += "gameId=" + gameId;
    postJson += "&sid=" + sid;
    postJson += "&roleId=" + roleId;
    postJson += "&title=" + title;
    postJson += "&content=" + content;
    postJson += "&goodsList=" + goodsList;
    let sign = JHttp.md5(postJson);
    postJson["sign"] = sign;
    let post = JHttp.post("http://" + host + ":" + webPort + "/sendRole", postJson);
}