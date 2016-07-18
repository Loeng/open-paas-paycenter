package cn.com.open.openpaas.payservice.app.channel.alipay;

public enum PaymentType{
    //通过括号赋值,而且必须带有一个参构造器和一个属性跟方法，否则编译出错
    //赋值必须都赋值或都不赋值，不能一部分赋值一部分不赋值；如果不赋值则不能写构造器，赋值编译也出错
    ALI(10001), WEIXIN(10002),ALIFTF(10003),
    TCLALIPAY(10004),UPOP(10006),PAB(10016),ECITIC(10017),CGB(10013),ICBC(10008),
    CMB(10007),BOS(10018),PSBC(10012),BCOM(10004),SPDB(10014)
    ,SDB(10019),BOB(10020),ABC(10010),HXB(10021),CCB(10009),
    CEB(10015),CMBC(10022),CIB(10023),BOC(10011),MICBC(10024),
    MCMB(10025),MABC(10026),MCCB(10027),MCEB(10028),MBOC(10029),
    MSPDB(10030),MBCOM(10031),MPAB(10032),TCLWEIXIN(10005);
    
    private final Integer value;

    //构造器默认也只能是private, 从而保证构造函数只能在内部使用
    PaymentType(Integer value) {
        this.value = value;
    }
    
    public Integer getValue() {
        return value;
    }
}
