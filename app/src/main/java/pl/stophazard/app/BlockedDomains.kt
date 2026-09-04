package pl.stophazard.app

object BlockedDomains {
    private val builtIn=setOf(
        "bet365.com","betway.com","888.com","888sport.com","williamhill.com","unibet.com",
        "bwin.com","pokerstars.com","betfair.com","ladbrokes.com","coral.co.uk","skybet.com",
        "betsson.com","leovegas.com","mrgreen.com","casumo.com","paddypower.com","10bet.com",
        "betvictor.com","sportingbet.com","22bet.com","1xbet.com","melbet.com","parimatch.com",
        "betano.com","stake.com","roobet.com","bc.game","cloudbet.com","rollbit.com",
        "gamdom.com","duelbits.com","gg.bet","fortunejack.com","888casino.com",
        "partypoker.com","pokerstarscasino.com","casino.com","videoslots.com","jackpotcity.com",
        "royalpanda.com","spinpalace.com","betonline.ag","sportsbetting.ag","mybookie.ag",
        "superbahis.com","1win.pro","mostbet.com"
    )
    fun normalize(host:String)=host.trim().lowercase().removePrefix("www.").substringBefore("/")
    fun isBlocked(host:String,custom:Set<String> = emptySet()):Boolean {
        val h=normalize(host)
        return (builtIn+custom.map(::normalize)).any{h==it || h.endsWith("."+it)}
    }
    fun all():Set<String> = builtIn
}
