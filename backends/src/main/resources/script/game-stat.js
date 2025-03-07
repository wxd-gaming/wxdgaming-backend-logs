function GameStat(gameId, gameStat) {
    let sql = `
        SELECT \"count\"(DISTINCT ll.account)
        FROM record_account as ll
        WHERE ll.daykey = ?;
    `;
    var query = SqlQuery.executeScalar(gameId, sql, 20250225);
    console.log(query);
    gameStat.setArpu(query.toString());
    return gameStat;
}