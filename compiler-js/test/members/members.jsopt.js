var $$$cl15=require('ceylon/language/0.1/ceylon.language');

//MethodDefinition expect at members.ceylon (1:0-8:0)
function expect(actual,expected,text){
    if ((actual.equals(expected))===$$$cl15.getTrue()){
        $$$cl15.print($$$cl15.String("[ok] ").plus(text).plus($$$cl15.String(": '")).plus(actual.getString()).plus($$$cl15.String("'")));
    }
    else {
        $$$cl15.print($$$cl15.String("[NOT OK] ").plus(text).plus($$$cl15.String(": actual='")).plus(actual.getString()).plus($$$cl15.String("', expected='")).plus(expected.getString()).plus($$$cl15.String("'")));
    }
    
}

//ClassDefinition Counter at members.ceylon (10:0-24:0)
function $Counter(){}
for(var $ in CeylonObject.prototype){$Counter.prototype[$]=CeylonObject.prototype[$]}
for(var $ in CeylonObject.prototype){$Counter.prototype[$+'$']=CeylonObject.prototype[$]}

//AttributeDeclaration currentCount at members.ceylon (11:4-11:45)
$Counter.prototype.getCurrentCount=function getCurrentCount(){
    return this.currentCount;
}
$Counter.prototype.setCurrentCount=function setCurrentCount(currentCount){
    this.currentCount=currentCount;
}

//AttributeGetterDefinition count at members.ceylon (12:4-14:4)
$Counter.prototype.getCount=function getCount(){
    var $$counter=this;
    return $$counter.getCurrentCount();
}

//MethodDefinition inc at members.ceylon (15:4-17:4)
$Counter.prototype.inc=function inc(){
    var $$counter=this;
    $$counter.setCurrentCount($$counter.getCurrentCount().plus($$$cl15.Integer(1)));
}

//AttributeGetterDefinition initialCount at members.ceylon (18:4-20:4)
$Counter.prototype.getInitialCount=function getInitialCount(){
    var $$counter=this;
    return $$counter.initialCount;
}

//AttributeGetterDefinition string at members.ceylon (21:4-23:4)
$Counter.prototype.getString=function getString(){
    var $$counter=this;
    return $$$cl15.String("Counter[").plus($$counter.getCount().getString()).plus($$$cl15.String("]"));
}
function Counter(initialCount, $$counter){
    if ($$counter===undefined)$$counter=new $Counter;
    $$counter.initialCount=initialCount;
    
    //AttributeDeclaration currentCount at members.ceylon (11:4-11:45)
    $$counter.currentCount=$$counter.initialCount;
    return $$counter;
}
this.Counter=Counter;

//ClassDefinition Issue10C1 at members.ceylon (26:0-34:0)
function $Issue10C1(){}
for(var $ in CeylonObject.prototype){$Issue10C1.prototype[$]=CeylonObject.prototype[$]}
for(var $ in CeylonObject.prototype){$Issue10C1.prototype[$+'$']=CeylonObject.prototype[$]}

//AttributeDeclaration i1 at members.ceylon (27:4-27:18)
$Issue10C1.prototype.getI1=function getI1(){
    return this.i1;
}

//AttributeDeclaration i2 at members.ceylon (28:4-28:18)
$Issue10C1.prototype.getI2=function getI2(){
    return this.i2;
}

//AttributeDeclaration i3 at members.ceylon (29:4-29:33)
$Issue10C1.prototype.getI3=function getI3(){
    return this.i3;
}

//MethodDefinition f1 at members.ceylon (30:4-30:39)
$Issue10C1.prototype.f1=function f1(){
    var $$issue10C1=this;
    return $$issue10C1.arg1;
}

//MethodDefinition f2 at members.ceylon (31:4-31:37)
$Issue10C1.prototype.f2=function f2(){
    var $$issue10C1=this;
    return $$issue10C1.getI1();
}

//MethodDefinition f3 at members.ceylon (32:4-32:37)
$Issue10C1.prototype.f3=function f3(){
    var $$issue10C1=this;
    return $$issue10C1.getI2();
}

//MethodDefinition f4 at members.ceylon (33:4-33:37)
$Issue10C1.prototype.f4=function f4(){
    var $$issue10C1=this;
    return $$issue10C1.getI3();
}
function Issue10C1(arg1, $$issue10C1){
    if ($$issue10C1===undefined)$$issue10C1=new $Issue10C1;
    $$issue10C1.arg1=arg1;
    
    //AttributeDeclaration i1 at members.ceylon (27:4-27:18)
    $$issue10C1.i1=$$$cl15.Integer(3);
    
    //AttributeDeclaration i2 at members.ceylon (28:4-28:18)
    $$issue10C1.i2=$$$cl15.Integer(5);
    
    //AttributeDeclaration i3 at members.ceylon (29:4-29:33)
    $$issue10C1.i3=$$$cl15.Integer(7);
    return $$issue10C1;
}

//ClassDefinition Issue10C2 at members.ceylon (35:0-41:0)
function $Issue10C2(){}
for(var $ in $Issue10C1.prototype){$Issue10C2.prototype[$]=$Issue10C1.prototype[$]}
for(var $ in $Issue10C1.prototype){$Issue10C2.prototype[$+'$']=$Issue10C1.prototype[$]}

//AttributeDeclaration i1 at members.ceylon (36:4-36:18)
$Issue10C2.prototype.getI1=function getI1(){
    return this.i1;
}

//AttributeDeclaration i2 at members.ceylon (37:4-37:25)
$Issue10C2.prototype.getI2=function getI2(){
    return this.i2;
}

//AttributeDeclaration i3 at members.ceylon (38:4-38:32)
$Issue10C2.prototype.getI3=function getI3(){
    return this.i3;
}

//MethodDefinition f5 at members.ceylon (39:4-39:39)
$Issue10C2.prototype.f5=function f5(){
    var $$issue10C2=this;
    return $$issue10C2.arg1;
}

//MethodDefinition f6 at members.ceylon (40:4-40:37)
$Issue10C2.prototype.f6=function f6(){
    var $$issue10C2=this;
    return $$issue10C2.getI1();
}
function Issue10C2(arg1, $$issue10C2){
    if ($$issue10C2===undefined)$$issue10C2=new $Issue10C2;
    Issue10C1($$$cl15.Integer(1),$$issue10C2);
    $$issue10C2.arg1=arg1;
    
    //AttributeDeclaration i1 at members.ceylon (36:4-36:18)
    $$issue10C2.i1=$$$cl15.Integer(4);
    
    //AttributeDeclaration i2 at members.ceylon (37:4-37:25)
    $$issue10C2.i2=$$$cl15.Integer(6);
    
    //AttributeDeclaration i3 at members.ceylon (38:4-38:32)
    $$issue10C2.i3=$$$cl15.Integer(8);
    return $$issue10C2;
}

//MethodDefinition testIssue10 at members.ceylon (43:0-53:0)
function testIssue10(){
    
    //AttributeDeclaration obj at members.ceylon (44:4-44:28)
    var $obj=Issue10C2($$$cl15.Integer(2));
    function getObj(){
        return $obj;
    }
    expect(getObj().f1(),$$$cl15.Integer(1),$$$cl15.String("Issue #10 (parameter)"));
    expect(getObj().f5(),$$$cl15.Integer(2),$$$cl15.String("Issue #10 (parameter)"));
    expect(getObj().f2(),$$$cl15.Integer(3),$$$cl15.String("Issue #10 (non-shared attribute)"));
    expect(getObj().f6(),$$$cl15.Integer(4),$$$cl15.String("Issue #10 (non-shared attribute)"));
    expect(getObj().f3(),$$$cl15.Integer(5),$$$cl15.String("Issue #10 (non-shared attribute)"));
    expect(getObj().getI2(),$$$cl15.Integer(6),$$$cl15.String("Issue #10 (shared attribute)"));
    expect(getObj().f4(),$$$cl15.Integer(8),$$$cl15.String("Issue #10 (shared attribute)"));
    expect(getObj().getI3(),$$$cl15.Integer(8),$$$cl15.String("Issue #10 (shared attribute)"));
}

//MethodDefinition test at members.ceylon (55:0-63:0)
function test(){
    
    //AttributeDeclaration c at members.ceylon (56:4-56:24)
    var $c=Counter($$$cl15.Integer(0));
    function getC(){
        return $c;
    }
    $$$cl15.print(getC().getCount());
    getC().inc();
    getC().inc();
    $$$cl15.print(getC().getCount());
    $$$cl15.print(getC());
    testIssue10();
}
this.test=test;
