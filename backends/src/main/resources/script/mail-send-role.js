function sendRole(host, webPort, gameId, sid, roleId, title, content, goodsList) {
    let postJson = "";
    postJson += "gameId=" + gameId;
    postJson += "&sid=" + sid;
    postJson += "&roleId=" + roleId;
    postJson += "&title=" + title;
    postJson += "&content=" + content;
    postJson += "&goodsList=" + goodsList;
    postJson += "&sign=" + JHttp.md5(postJson);
    let post = JHttp.post("http://" + host + ":" + webPort + "/sendRole", postJson);
    if (post.code === 1) {
        console.log("发送成功");
    } else {
        console.log("发送失败：" + post.msg);
    }
}