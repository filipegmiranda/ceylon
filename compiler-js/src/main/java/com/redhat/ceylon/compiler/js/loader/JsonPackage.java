package com.redhat.ceylon.compiler.js.loader;

import static com.redhat.ceylon.compiler.js.loader.MetamodelGenerator.KEY_ANNOTATIONS;
import static com.redhat.ceylon.compiler.js.loader.MetamodelGenerator.KEY_ATTRIBUTES;
import static com.redhat.ceylon.compiler.js.loader.MetamodelGenerator.KEY_CLASSES;
import static com.redhat.ceylon.compiler.js.loader.MetamodelGenerator.KEY_CONSTRUCTOR;
import static com.redhat.ceylon.compiler.js.loader.MetamodelGenerator.KEY_CONSTRUCTORS;
import static com.redhat.ceylon.compiler.js.loader.MetamodelGenerator.KEY_DEFAULT;
import static com.redhat.ceylon.compiler.js.loader.MetamodelGenerator.KEY_DS_VARIANCE;
import static com.redhat.ceylon.compiler.js.loader.MetamodelGenerator.KEY_DYNAMIC;
import static com.redhat.ceylon.compiler.js.loader.MetamodelGenerator.KEY_FLAGS;
import static com.redhat.ceylon.compiler.js.loader.MetamodelGenerator.KEY_INTERFACES;
import static com.redhat.ceylon.compiler.js.loader.MetamodelGenerator.KEY_JS_TSENUM;
import static com.redhat.ceylon.compiler.js.loader.MetamodelGenerator.KEY_METATYPE;
import static com.redhat.ceylon.compiler.js.loader.MetamodelGenerator.KEY_METHODS;
import static com.redhat.ceylon.compiler.js.loader.MetamodelGenerator.KEY_MODULE;
import static com.redhat.ceylon.compiler.js.loader.MetamodelGenerator.KEY_NAME;
import static com.redhat.ceylon.compiler.js.loader.MetamodelGenerator.KEY_OBJECTS;
import static com.redhat.ceylon.compiler.js.loader.MetamodelGenerator.KEY_PACKAGE;
import static com.redhat.ceylon.compiler.js.loader.MetamodelGenerator.KEY_PACKED_ANNS;
import static com.redhat.ceylon.compiler.js.loader.MetamodelGenerator.KEY_PARAMS;
import static com.redhat.ceylon.compiler.js.loader.MetamodelGenerator.KEY_SATISFIES;
import static com.redhat.ceylon.compiler.js.loader.MetamodelGenerator.KEY_SELF_TYPE;
import static com.redhat.ceylon.compiler.js.loader.MetamodelGenerator.KEY_TYPE;
import static com.redhat.ceylon.compiler.js.loader.MetamodelGenerator.KEY_TYPES;
import static com.redhat.ceylon.compiler.js.loader.MetamodelGenerator.KEY_TYPE_ARGS;
import static com.redhat.ceylon.compiler.js.loader.MetamodelGenerator.KEY_TYPE_PARAMS;
import static com.redhat.ceylon.compiler.js.loader.MetamodelGenerator.KEY_US_VARIANCE;
import static com.redhat.ceylon.compiler.js.loader.MetamodelGenerator.METATYPE_ALIAS;
import static com.redhat.ceylon.compiler.js.loader.MetamodelGenerator.METATYPE_ATTRIBUTE;
import static com.redhat.ceylon.compiler.js.loader.MetamodelGenerator.METATYPE_CLASS;
import static com.redhat.ceylon.compiler.js.loader.MetamodelGenerator.METATYPE_GETTER;
import static com.redhat.ceylon.compiler.js.loader.MetamodelGenerator.METATYPE_INTERFACE;
import static com.redhat.ceylon.compiler.js.loader.MetamodelGenerator.METATYPE_METHOD;
import static com.redhat.ceylon.compiler.js.loader.MetamodelGenerator.METATYPE_OBJECT;
import static com.redhat.ceylon.compiler.js.loader.MetamodelGenerator.annotationBits;
import static com.redhat.ceylon.compiler.js.loader.MetamodelGenerator.partiallyQualifiedName;
import static com.redhat.ceylon.model.typechecker.model.ModelUtil.getSignature;
import static com.redhat.ceylon.model.typechecker.model.ModelUtil.isVariadic;
import static com.redhat.ceylon.model.typechecker.model.Module.LANGUAGE_MODULE_NAME;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.redhat.ceylon.common.Backend;
import com.redhat.ceylon.common.Backends;
import com.redhat.ceylon.compiler.js.CompilerErrorException;
import com.redhat.ceylon.model.typechecker.model.Annotation;
import com.redhat.ceylon.model.typechecker.model.ClassAlias;
import com.redhat.ceylon.model.typechecker.model.ClassOrInterface;
import com.redhat.ceylon.model.typechecker.model.Constructor;
import com.redhat.ceylon.model.typechecker.model.Declaration;
import com.redhat.ceylon.model.typechecker.model.Function;
import com.redhat.ceylon.model.typechecker.model.FunctionOrValue;
import com.redhat.ceylon.model.typechecker.model.Generic;
import com.redhat.ceylon.model.typechecker.model.Interface;
import com.redhat.ceylon.model.typechecker.model.InterfaceAlias;
import com.redhat.ceylon.model.typechecker.model.IntersectionType;
import com.redhat.ceylon.model.typechecker.model.ModelUtil;
import com.redhat.ceylon.model.typechecker.model.NothingType;
import com.redhat.ceylon.model.typechecker.model.Parameter;
import com.redhat.ceylon.model.typechecker.model.ParameterList;
import com.redhat.ceylon.model.typechecker.model.Scope;
import com.redhat.ceylon.model.typechecker.model.Setter;
import com.redhat.ceylon.model.typechecker.model.SiteVariance;
import com.redhat.ceylon.model.typechecker.model.Type;
import com.redhat.ceylon.model.typechecker.model.TypeAlias;
import com.redhat.ceylon.model.typechecker.model.TypeDeclaration;
import com.redhat.ceylon.model.typechecker.model.TypeParameter;
import com.redhat.ceylon.model.typechecker.model.UnionType;
import com.redhat.ceylon.model.typechecker.model.Unit;
import com.redhat.ceylon.model.typechecker.model.UnknownType;
import com.redhat.ceylon.model.typechecker.model.Value;
import com.redhat.ceylon.model.typechecker.util.ModuleManager;

public class JsonPackage extends LazyPackage {

    //Ugly hack to have a ref to Basic at hand, to use as implicit supertype of classes
    private final static Map<String,Object> idobj = new HashMap<>();
    //This is to use as the implicit supertype of interfaces
    private final static Map<String,Object> objclass = new HashMap<>();
    //This is for type parameters
    private final static Map<String,Object> voidclass = new HashMap<>();
    private Map<String,Object> model;
    private final String pkgname;
    private boolean loaded = false;
    private final Unit u2 = new Unit();
    private NothingType nothing = new NothingType(u2);
    private UnknownType unknown = new UnknownType(u2);

    static {
        idobj.put(KEY_NAME, "Basic");
        idobj.put(KEY_PACKAGE, LANGUAGE_MODULE_NAME);
        idobj.put(KEY_MODULE, LANGUAGE_MODULE_NAME);
        objclass.put(KEY_NAME, "Object");
        objclass.put(KEY_PACKAGE, LANGUAGE_MODULE_NAME);
        objclass.put(KEY_MODULE, LANGUAGE_MODULE_NAME);
        voidclass.put(KEY_NAME, "Anything");
        voidclass.put(KEY_PACKAGE, LANGUAGE_MODULE_NAME);
        voidclass.put(KEY_MODULE, LANGUAGE_MODULE_NAME);
    }
    public JsonPackage(String pkgname) {
        this.pkgname = pkgname;
        setName(ModuleManager.splitModuleName(pkgname));
    }

    boolean inLoadIfNecessary = false;
    protected void loadIfNecessary() {
        // model will be null if this is a module that we are currently compiling
        if (!inLoadIfNecessary && !loaded && null != model) {
            inLoadIfNecessary = true;
            loadDeclarations();
            inLoadIfNecessary = false;
        }
    }

    public void setModule(com.redhat.ceylon.model.typechecker.model.Module module) {
        if (module instanceof JsonModule && model == null) {
            model = ((JsonModule)module).getModelForPackage(getNameAsString());
            u2.setPackage(this);
            u2.setFilename("");
            u2.setFullPath("");
            u2.setRelativePath("");
            addUnit(u2);
            //Annotations
            if (model != null) {
                if (model.get("$pkg-pa") != null) {
                    int bits = (int)model.remove("$pkg-pa");
                    setShared(hasAnnotationBit(bits, "shared"));
                }
                setPackageAnnotations(model.remove("$pkg-anns"));
            }

            // This was part of loadDeclarations() which is now being called lazily, but
            // needs to run eagerly, for whatever reason.

            if (module.getLanguageModule() == module && LANGUAGE_MODULE_NAME.equals(pkgname)) {
                //Mark the language module as immediately available to bypass certain validations
                module.setAvailable(true);
            }
        }

        super.setModule(module);
    }

    @SuppressWarnings("unchecked")
    private void setPackageAnnotations(Object pkgAnns) {
        if (pkgAnns instanceof List) {
            JsonPackage.setNewAnnotations(getAnnotations(), (List<Map<String,List<String>>>)pkgAnns);
        } else if (pkgAnns instanceof Map) {
            JsonPackage.setOldAnnotations(getAnnotations(), (Map<String,List<String>>)pkgAnns);
        } else if (pkgAnns != null) {
            throw new IllegalArgumentException("Annotations should be a List (new format) or a Map (old format)");
        }
    };
    
    Map<String,Object> getModel() { return model; }

    private void loadDeclarations() {
        if (loaded) return;
        loaded = true;
        if (!isShared()) {
            setShared(model.remove("$pkg-shared") != null);
        }
        for (Map.Entry<String,Object> e : model.entrySet()) {
            String k = e.getKey();
            if (!k.startsWith("$pkg-")) {
                @SuppressWarnings("unchecked")
                Map<String,Object> m = (Map<String,Object>)e.getValue();
                if (m.get(KEY_METATYPE) instanceof String) {
                    String metatype = (String)m.get(KEY_METATYPE);
                    if (METATYPE_CLASS.equals(metatype)) {
                        refineMembers(loadClass(e.getKey(), m, this, null));
                    } else if (METATYPE_INTERFACE.equals(metatype)) {
                        refineMembers(loadInterface(e.getKey(), m, this, null));
                    } else if (metatype.equals(METATYPE_ATTRIBUTE)
                            || metatype.equals(METATYPE_GETTER)) {
                        loadAttribute(k, m, this, null);
                    } else if (metatype.equals(METATYPE_METHOD)) {
                        loadMethod(k, m, this, null);
                    } else if (metatype.equals(METATYPE_OBJECT)) {
                        refineMembers((com.redhat.ceylon.model.typechecker.model.Class)loadObject(k, m, this, null));
                    } else if (metatype.equals(METATYPE_ALIAS)) {
                        loadTypeAlias(k, m, this, null);
                    }
                } else if (m.get(KEY_METATYPE) == null) {
                    throw new IllegalArgumentException("Missing metatype from entry " + m + " under " + e.getKey());
                } else if (m.get(KEY_METATYPE) instanceof ClassOrInterface) {
                    refineMembers((ClassOrInterface)m.get(KEY_METATYPE));
                } else if (m.get(MetamodelGenerator.KEY_METATYPE) instanceof Value) {
                    TypeDeclaration td = ((Value)m.get(MetamodelGenerator.KEY_METATYPE)).getTypeDeclaration();
                    if (td != null) {
                        refineMembers((ClassOrInterface)td);
                    }
                }
            }
        }
    }

    /** Loads a class from the specified map. To avoid circularities, when the class is being created it is
     * added to the map, and once it's been fully loaded, all other keys are removed. */
    @SuppressWarnings("unchecked")
    com.redhat.ceylon.model.typechecker.model.Class loadClass(String name, Map<String, Object> m,
            Scope parent, final List<TypeParameter> existing) {
        com.redhat.ceylon.model.typechecker.model.Class cls;
        m.remove(KEY_NAME);
        if (m.get(KEY_METATYPE) instanceof com.redhat.ceylon.model.typechecker.model.Class) {
            cls = (com.redhat.ceylon.model.typechecker.model.Class)m.get(KEY_METATYPE);
            if (m.size() <= 3) {
                //It's been fully loaded
                return cls;
            }
        } else {
            //It's not there, so create it
            if (m.containsKey("$alias")) {
                cls = new com.redhat.ceylon.model.typechecker.model.ClassAlias();
            } else {
                cls = new com.redhat.ceylon.model.typechecker.model.Class();
            }
            cls.setAbstract(m.remove("abstract") != null);
            cls.setAnonymous(m.remove("$anon") != null);
            cls.setDynamic(m.remove(KEY_DYNAMIC) != null);
            cls.setContainer(parent);
            cls.setScope(parent);
            cls.setName(name);
            cls.setUnit(u2);
            if (parent == this) {
                u2.addDeclaration(cls);
            }
            parent.addMember(cls);
            m.put(KEY_METATYPE, cls);
            setAnnotations(cls, (Integer)m.remove(KEY_PACKED_ANNS), m.remove(KEY_ANNOTATIONS));
        }
        //Type parameters are about the first thing we need to load
        final List<TypeParameter> tparms = parseTypeParameters(
                (List<Map<String,Object>>)m.remove(KEY_TYPE_PARAMS), cls, existing);
        final List<TypeParameter> allparms = JsonPackage.merge(tparms, existing);
        if (m.containsKey(KEY_SELF_TYPE)) {
            for (TypeParameter t : tparms) {
                if (t.getName().equals(m.get(KEY_SELF_TYPE))) {
                    cls.setSelfType(t.getType());
                }
            }
        }
        //This is to avoid circularity
        if (!(isLanguagePackage() && ("Nothing".equals(name) || "Anything".equals(name)))) {
            if (cls.getExtendedType() == null) {
                if (m.containsKey("super")) {
                    Type father = getTypeFromJson((Map<String,Object>)m.get("super"),
                            parent instanceof Declaration ? (Declaration)parent : null, allparms);
                    if (father != null) {
                        m.remove("super");
                        cls.setExtendedType(father);
                    }
                } else {
                    cls.setExtendedType(getTypeFromJson(idobj,
                            parent instanceof Declaration ? (Declaration)parent : null, allparms));
                }
            }
        }

        if (cls instanceof ClassAlias) {
            ClassAlias ca = (ClassAlias) cls;
            if (m.containsKey(KEY_CONSTRUCTOR)) {
                String constructorName = (String) m.get(KEY_CONSTRUCTOR);
                Function ctorFn = (Function) ca.getExtendedType().getDeclaration().getDirectMember(constructorName, null, false);
                ca.setConstructor(ctorFn.getType().getDeclaration());
            }
            else {
                ca.setConstructor(ca.getExtendedType().getDeclaration());
            }
        }
        if (m.containsKey(KEY_CONSTRUCTORS)) {
            final Map<String,Map<String,Object>> constructors = (Map<String,Map<String,Object>>)m.remove(
                    KEY_CONSTRUCTORS);
            for (Map.Entry<String, Map<String,Object>> cons : constructors.entrySet()) {
                Constructor cnst = new Constructor();
                cnst.setName("$def".equals(cons.getKey())?null:cons.getKey());
                cnst.setContainer(cls);
                cnst.setScope(cls);
                cnst.setUnit(cls.getUnit());
                cnst.setExtendedType(cls.getType());
                cnst.setDynamic(cons.getValue().remove(KEY_DYNAMIC) != null);
                setAnnotations(cnst, (Integer)cons.getValue().remove(KEY_PACKED_ANNS),
                        cons.getValue().remove(KEY_ANNOTATIONS));
                final List<Map<String,Object>> modelPlist = (List<Map<String,Object>>)cons.getValue().remove(
                        KEY_PARAMS);
                cls.addMember(cnst);
                if (modelPlist == null) {
                    //It's a value constructor
                    cls.setEnumerated(true);
                    Value cv = new Value();
                    cv.setName(cnst.getName());
                    cv.setType(cnst.getType());
                    cv.setContainer(cls);
                    cv.setScope(cls);
                    cv.setUnit(cls.getUnit());
                    cv.setVisibleScope(cls.getVisibleScope());
                    cv.setShared(cls.isShared());
                    cv.setDeprecated(cls.isDeprecated());
                    cls.addMember(cv);
                } else {
                    cls.setConstructors(true);
                    final ParameterList plist = parseParameters(modelPlist, cnst, allparms);
                    cnst.addParameterList(plist);
                    plist.setNamedParametersSupported(true);
                    Function cf = new Function();
                    cf.setName(cnst.getName());
                    final Type ft = cnst.appliedType(cnst.getExtendedType(), Collections.<Type>emptyList());
                    cf.setType(ft);
                    cf.addParameterList(plist);
                    cf.setContainer(cls);
                    cf.setScope(cls);
                    cf.setUnit(cls.getUnit());
                    cf.setVisibleScope(cnst.getVisibleScope());
                    cf.setShared(cnst.isShared());
                    cf.setDeprecated(cnst.isDeprecated());
                    cf.setDynamic(cnst.isDynamic());
                    cls.addMember(cf);
                }
                if (cons.getValue().containsKey(KEY_JS_TSENUM)) {
                    cnst.setTypescriptEnum((String)cons.getValue().get(KEY_JS_TSENUM));
                }
            }
        } else {
            ParameterList plist = parseParameters((List<Map<String,Object>>)m.remove(KEY_PARAMS),
                    cls, allparms);
            plist.setNamedParametersSupported(true);
            cls.setParameterList(plist);
        }
        if (m.containsKey("of") && cls.getCaseTypes() == null) {
            cls.setCaseTypes(parseTypeList((List<Map<String,Object>>)m.get("of"), allparms));
            m.remove("of");
        }
        if (m.containsKey(KEY_SATISFIES)) {
            List<Map<String,Object>> stypes = (List<Map<String,Object>>)m.remove(KEY_SATISFIES);
            cls.setSatisfiedTypes(parseTypeList(stypes, allparms));
        }
        if (m.containsKey(KEY_OBJECTS)) {
            for (Map.Entry<String,Map<String,Object>> inner : ((Map<String,Map<String,Object>>)m.get(KEY_OBJECTS)).entrySet()) {
                loadObject(inner.getKey(), inner.getValue(), cls, allparms);
            }
            m.remove(KEY_OBJECTS);
        }
        addAttributesAndMethods(m, cls, allparms);
        if (m.containsKey(KEY_INTERFACES)) {
            Map<String,Map<String,Object>> cdefs = (Map<String,Map<String,Object>>)m.get(KEY_INTERFACES);
            for (Map.Entry<String,Map<String,Object>> cdef : cdefs.entrySet()) {
                loadInterface(cdef.getKey(), cdef.getValue(), cls, allparms);
            }
            m.remove(KEY_INTERFACES);
        }
        if (m.containsKey(KEY_CLASSES)) {
            Map<String,Map<String,Object>> cdefs = (Map<String,Map<String,Object>>)m.get(KEY_CLASSES);
            for (Map.Entry<String,Map<String,Object>> cdef : cdefs.entrySet()) {
                loadClass(cdef.getKey(), cdef.getValue(), cls, allparms);
            }
            m.remove(KEY_CLASSES);
        }
        if (cls.isDynamic() &&
                (getModule().getJsMajor()<9 ||
                    (getModule().getJsMajor()==9 && getModule().getJsMinor()<1))) {
            // previous versions did not set dynamic flag on members
            cls.makeMembersDynamic();
        }
        return cls;
    }

    /** Parses the specified attributes and methods from JSON data and adds them to the specified declaration. */
    @SuppressWarnings("unchecked")
    private void addAttributesAndMethods(Map<String,Object> m, Declaration d, List<TypeParameter> tparms) {
        //Attributes
        Map<String, Map<String,Object>> sub = (Map<String,Map<String,Object>>)m.get(KEY_ATTRIBUTES);
        if (sub != null) {
            //Only add aliases in the first pass
            for(Map.Entry<String, Map<String,Object>> e : sub.entrySet()) {
                if (d.getDirectMember(e.getKey(), null, false) == null) {
                    if (METATYPE_ALIAS.equals(e.getValue().get(KEY_METATYPE))) {
                        d.getMembers().add(loadTypeAlias(e.getKey(), e.getValue(), (Scope)d, tparms));
                    }
                }
            }
            //Then the attributes
            for(Map.Entry<String, Map<String,Object>> e : sub.entrySet()) {
                if (d.getDirectMember(e.getKey(), null, false) == null) {
                    if (!METATYPE_ALIAS.equals(e.getValue().get(KEY_METATYPE))) {
                        d.getMembers().add(loadAttribute(e.getKey(), e.getValue(), (Scope)d, tparms));
                    }
                }
            }
        }
        //Methods
        sub = (Map<String,Map<String,Object>>)m.get(KEY_METHODS);
        if (sub != null) {
            for(Map.Entry<String, Map<String,Object>> e : sub.entrySet()) {
                if (d.getDirectMember(e.getKey(), null, false) == null) {
                    d.getMembers().add(loadMethod(e.getKey(), e.getValue(), (Scope)d, tparms));
                }
            }
        }
    }

    /** Creates a list of Type from the references in the maps.
     * @param types A list of maps where each map is a reference to a type or type parameter.
     * @param typeParams The type parameters that can be referenced from the list of maps. */
    private List<Type> parseTypeList(List<Map<String,Object>> types, List<TypeParameter> typeParams) {
        List<Type> ts = new ArrayList<>(types.size());
        for (Map<String,Object> st : types) {
            ts.add(getTypeFromJson(st, null, typeParams));
        }
        return ts;
    }

    /** Creates a list of TypeParameter from a list of maps.
     * @param typeParams The list of maps to create the TypeParameters.
     * @param container The declaration which owns the resulting type parameters.
     * @param existing A list of type parameters declared in the parent scopes which can be referenced from
     * the ones that have to be parsed. */
    private List<TypeParameter> parseTypeParameters(List<Map<String,Object>> typeParams, final Declaration container,
            List<TypeParameter> existing) {
        if (typeParams == null) return Collections.emptyList();
        //New array with existing parms to avoid modifying that one
        List<TypeParameter> allparms = new ArrayList<>((existing == null ? 0 : existing.size()) + typeParams.size());
        if (existing != null && !existing.isEmpty()) {
            allparms.addAll(existing);
        }
        List<TypeParameter> tparms = new ArrayList<>(typeParams.size());
        //To avoid circularity, this is done in two phases:
        //First create the type parameters
        for (Map<String,Object> tp : typeParams) {
            final Declaration maybe;
            if (tp.get(KEY_METATYPE) instanceof TypeParameter) {
                maybe = (TypeParameter)tp.get(KEY_METATYPE);
            } else {
                maybe = container.getDirectMember((String)tp.get(KEY_NAME), null, false);
            }
            if (maybe instanceof TypeParameter) {
                //we already had it (from partial loading elsewhere)
                allparms.add((TypeParameter)maybe);
                tparms.add((TypeParameter)maybe);
                tp.put(KEY_METATYPE, maybe);
            } else {
                TypeParameter tparm = new TypeParameter();
                tparm.setUnit(container.getUnit());
                tparm.setDeclaration(container);
                container.getMembers().add(tparm);
                if (tp.containsKey(KEY_NAME)) {
                    tparm.setName((String)tp.get(KEY_NAME));
                } else if (!tp.containsKey(KEY_TYPES)) {
                    throw new IllegalArgumentException("Invalid type parameter map " + tp);
                }
                String variance = (String)tp.get(KEY_DS_VARIANCE);
                if ("out".equals(variance)) {
                    tparm.setCovariant(true);
                } else if ("in".equals(variance)) {
                    tparm.setContravariant(true);
                }
                if (container instanceof Scope) {
                    Scope scope = (Scope)container;
                    tparm.setContainer(scope);
                    tparm.setScope(scope);
                }
                tparm.setDefaulted(tp.containsKey(KEY_DEFAULT));
                tparms.add(tparm);
                allparms.add(tparm);
                tp.put(KEY_METATYPE, tparm);
            }
        }
        if (container instanceof Generic) {
            ((Generic) container).setTypeParameters(tparms);
        }
        //Second, add defaults and heritage
        for (Map<String,Object> tp : typeParams) {
            TypeParameter tparm = (TypeParameter)tp.get(KEY_METATYPE);
            if (tparm.getExtendedType() == null) {
                if (tp.containsKey(KEY_PACKAGE)) {
                    //Looks like this never happens but...
                    Type subtype = getTypeFromJson(tp, container, allparms);
                    tparm.setExtendedType(subtype);
                } else if (tp.containsKey(KEY_TYPES)) {
                    if (!("u".equals(tp.get("comp")) || "i".equals(tp.get("comp")))) {
                        throw new IllegalArgumentException("Only union or intersection types are allowed as 'comp'");
                    }
                    Type subtype = getTypeFromJson(tp, container, allparms);
                    tparm.setName(subtype.asString());
                    tparm.setExtendedType(subtype);
                } else {
                    tparm.setExtendedType(getTypeFromJson(voidclass, container, null));
                }
            }
            if (tparm.isDefaulted()) {
                @SuppressWarnings("unchecked")
                final Map<String,Object> deftype = (Map<String,Object>)tp.get(KEY_DEFAULT);
                tparm.setDefaultTypeArgument(getTypeFromJson(deftype, container, existing));
            }
            if (tp.containsKey(KEY_SATISFIES)) {
                @SuppressWarnings("unchecked")
                final List<Map<String,Object>> stypes = (List<Map<String,Object>>)tp.get(KEY_SATISFIES);
                tparm.setSatisfiedTypes(parseTypeList(stypes, allparms));
                tparm.setConstrained(true);
            } else if (tp.containsKey("of")) {
                @SuppressWarnings("unchecked")
                final List<Map<String,Object>> oftype = (List<Map<String,Object>>)tp.get("of");
                tparm.setCaseTypes(parseTypeList(oftype, allparms));
                tparm.setConstrained(true);
            }
        }
        return tparms;
    }

    /** Creates a parameter list from a list of maps where each map represents a parameter.
     * @param params The list of maps to create the parameters.
     * @param owner The declaration to assign to each parameter.
     * @param typeParameters The type parameters which can be referenced from the parameters. */
    private ParameterList parseParameters(List<Map<String,Object>> params, Declaration owner, List<TypeParameter> typeParameters) {
        ParameterList plist = new ParameterList();
        if (params != null) {
            for (Map<String,Object> p : params) {
                Parameter param = new Parameter();
                final String paramtype = (String)p.get("$pt");
                param.setHidden(p.containsKey("$hdn"));
                param.setName((String)p.get(KEY_NAME));
                param.setDeclaration(owner);
                param.setDefaulted(p.containsKey(KEY_DEFAULT));
                param.setSequenced(p.containsKey("seq"));
                param.setAtLeastOne(p.containsKey("$min1"));
                if (paramtype == null || "v".equals(paramtype)) {
                    Value _v = new Value();
                    param.setModel(_v);
                } else if ("f".equals(paramtype)) {
                    @SuppressWarnings("unchecked")
                    List<List<Map<String,Object>>> paramLists = (List<List<Map<String,Object>>>)p.get(KEY_PARAMS);
                    Function _m = new Function();
                    param.setModel(_m);
                    if (paramLists == null) {
                        _m.addParameterList(new ParameterList());
                    } else {
                        boolean first = true;
                        for (List<Map<String,Object>> subplist : paramLists) {
                            ParameterList _params = parseParameters(subplist, _m, typeParameters);
                            if (first) {
                                first = false;
                            } else {
                                _params.setNamedParametersSupported(false);
                            }
                            _m.addParameterList(_params);
                        }
                    }
                } else {
                    throw new IllegalArgumentException("Unknown parameter type " + paramtype);
                }
                FunctionOrValue paramModel = param.getModel();
                if (paramModel != null) {
                    paramModel.setInitializerParameter(param);
                    paramModel.setName(param.getName());
                    paramModel.setUnit(u2);
                    if (owner instanceof Scope) {
                        Scope scope = (Scope)owner;
                        paramModel.setContainer(scope);
                        paramModel.setScope(scope);
                    }
                    if (p.get(KEY_TYPE) instanceof Map) {
                        @SuppressWarnings("unchecked")
                        final Map<String,Object> ktype = (Map<String,Object>)p.get(KEY_TYPE);
                        paramModel.setType(getTypeFromJson(ktype, owner, typeParameters));
                    } else {
                        //parameter type
                        for (TypeParameter tp : typeParameters) {
                            if (tp.getName().equals(p.get(KEY_TYPE))) {
                                paramModel.setType(tp.getType());
                            }
                        }
                    }
                    setAnnotations(paramModel, (Integer)p.remove(KEY_PACKED_ANNS), p.remove(KEY_ANNOTATIONS));
                }
                //owner.getMembers().add(param);
                plist.getParameters().add(param);
            }
        }
        return plist;
    }

    @SuppressWarnings("unchecked")
    Function loadMethod(String name, Map<String, Object> m, Scope parent, final List<TypeParameter> existing) {
        Function md = new Function();
        md.setName(name);
        m.remove(KEY_NAME);
        md.setContainer(parent);
        md.setScope(parent);
        setAnnotations(md, (Integer)m.remove(KEY_PACKED_ANNS), m.remove(KEY_ANNOTATIONS));
        md.setUnit(u2);
        if (parent == this) {
            //Top-level declarations are directly added to the unit
            u2.addDeclaration(md);
            addMember(null);
        }
        if (m.containsKey(KEY_FLAGS)) {
            int flags = (int)m.remove(KEY_FLAGS);
            md.setDeclaredVoid((flags & 1) > 0);
            md.setDeferred((flags & 2) > 0);
        }
        md.setDynamic(m.remove(KEY_DYNAMIC) != null);
        final List<TypeParameter> tparms = parseTypeParameters(
                (List<Map<String,Object>>)m.get(KEY_TYPE_PARAMS), md, existing);
        final List<TypeParameter> allparms = JsonPackage.merge(tparms, existing);
        md.setType(getTypeFromJson((Map<String,Object>)m.remove(KEY_TYPE),
                parent instanceof Declaration ? (Declaration)parent : null, allparms));
        List<List<Map<String,Object>>> paramLists = (List<List<Map<String,Object>>>)m.remove(KEY_PARAMS);
        if (paramLists == null) {
            md.addParameterList(new ParameterList());
        } else {
            boolean first = true;
            for (List<Map<String,Object>> plist : paramLists) {
                ParameterList _params = parseParameters(plist, md, allparms);
                _params.setNamedParametersSupported(first);
                first = false;
                md.addParameterList(_params);
                for (Parameter p : _params.getParameters()) {
                    md.addMember(p.getModel());
                }
            }
        }
        return md;
    }

    FunctionOrValue loadAttribute(String name, Map<String, Object> m, Scope parent,
            List<TypeParameter> typeParameters) {
        String metatype = (String)m.get(KEY_METATYPE);
        Value d = new Value();
        d.setTransient(METATYPE_GETTER.equals(metatype));
        d.setName(name);
        d.setContainer(parent);
        d.setScope(parent);
        d.setUnit(u2);
        if (parent == this) {
            u2.addDeclaration(d);
            addMember(null);
        }
        setAnnotations(d, (Integer)m.remove(KEY_PACKED_ANNS), m.remove(KEY_ANNOTATIONS));
        d.setDynamic(m.remove(KEY_DYNAMIC) != null);
        if (m.containsKey("var")) {
            d.setVariable(true);
        }
        @SuppressWarnings("unchecked")
        final Map<String,Object> ktype = (Map<String,Object>)m.get(KEY_TYPE);
        d.setType(getTypeFromJson(ktype, parent instanceof Declaration ? (Declaration)parent : null, typeParameters));
        @SuppressWarnings("unchecked")
        final Map<String, Object> smap = (Map<String, Object>)m.remove("$set");
        if (smap != null) {
            Setter s = new Setter();
            s.setName(name);
            s.setContainer(parent);
            s.setScope(parent);
            s.setUnit(u2);
            s.setGetter(d);
            d.setSetter(s);
            if (parent == this) {
                u2.addDeclaration(s);
                addMember(null);
            }
            setAnnotations(s, (Integer)smap.remove(KEY_PACKED_ANNS), smap.remove(KEY_ANNOTATIONS));
            s.setType(d.getType());
        }
        return d;
    }

    /** Sets the refined declarations for the type's members. */
    private void refineMembers(ClassOrInterface coi) {
        //fill refined declarations
        for (Declaration d : coi.getMembers()) {
            if (d.isActual()) {
                Declaration refined = coi.getRefinedMember(d.getName(), getSignature(d), isVariadic(d));
                if (refined==null) refined = d;
                d.setRefinedDeclaration(refined);
            }
            if (d instanceof ClassOrInterface) {
                refineMembers((ClassOrInterface)d);
            }
        }
    }

    @SuppressWarnings("unchecked")
    Interface loadInterface(String name, Map<String, Object> m, Scope parent, final List<TypeParameter> existing) {
        //Check if it's been loaded first
        //It hasn't been loaded, so create it
        Interface t;
        m.remove(KEY_NAME);
        if (m.get(KEY_METATYPE) instanceof Interface) {
            t = (Interface)m.get(KEY_METATYPE);
            if (m.size() <= 3) {
                //it's been loaded
                return t;
            }
        } else {
            if (m.containsKey("$alias")) {
                t = new InterfaceAlias();
            } else {
                t = new Interface();
            }
            t.setContainer(parent);
            t.setScope(parent);
            t.setName(name);
            t.setUnit(u2);
            if (parent == this) {
                u2.addDeclaration(t);
            }
            parent.addMember(t);
            m.put(KEY_METATYPE, t);
            setAnnotations(t, (Integer)m.remove(KEY_PACKED_ANNS), m.remove(KEY_ANNOTATIONS));
        }
        if (m.remove(KEY_DYNAMIC) != null) {
            t.setDynamic(true);
        }
        List<TypeParameter> tparms = t.getTypeParameters();
        List<Map<String,Object>> listOfMaps = (List<Map<String,Object>>)m.get(KEY_TYPE_PARAMS);
        if (listOfMaps != null && (tparms == null || tparms.size() < listOfMaps.size())) {
            tparms = parseTypeParameters(listOfMaps, t, existing);
            m.remove(KEY_TYPE_PARAMS);
        }
        final List<TypeParameter> allparms = JsonPackage.merge(tparms, existing);
        //All interfaces extend Object, except aliases
        if (t.getExtendedType() == null) {
            if (t.isAlias()) {
                t.setExtendedType(getTypeFromJson((Map<String,Object>)m.remove("$alias"),
                        parent instanceof Declaration ? (Declaration)parent : null, allparms));
            } else {
                t.setExtendedType(getTypeFromJson(objclass,
                        parent instanceof Declaration ? (Declaration)parent : null, null));
            }
        }
        if (m.containsKey(KEY_SELF_TYPE)) {
            for (TypeParameter _tp : tparms) {
                if (_tp.getName().equals(m.get(KEY_SELF_TYPE))) {
                    t.setSelfType(_tp.getType());
                    _tp.setSelfTypedDeclaration(t);
                }
            }
            m.remove(KEY_SELF_TYPE);
        }
        if (m.containsKey("of") && t.getCaseTypes() == null) {
            t.setCaseTypes(parseTypeList((List<Map<String,Object>>)m.remove("of"), allparms));
        }
        if (m.containsKey(KEY_SATISFIES)) {
            for (Type s : parseTypeList((List<Map<String,Object>>)m.remove(KEY_SATISFIES), allparms)) {
                t.getSatisfiedTypes().add(s);
            }
        }
        addAttributesAndMethods(m, t, allparms);
        if (m.containsKey(KEY_INTERFACES)) {
            Map<String,Map<String,Object>> cdefs = (Map<String,Map<String,Object>>)m.remove(KEY_INTERFACES);
            for (Map.Entry<String,Map<String,Object>> cdef : cdefs.entrySet()) {
                loadInterface(cdef.getKey(), cdef.getValue(), t, allparms);
            }
        }
        if (m.containsKey(KEY_CLASSES)) {
            Map<String,Map<String,Object>> cdefs = (Map<String,Map<String,Object>>)m.remove(KEY_CLASSES);
            for (Map.Entry<String,Map<String,Object>> cdef : cdefs.entrySet()) {
                loadClass(cdef.getKey(), cdef.getValue(), t, allparms);
            }
        }
        if (t.isDynamic() &&
                (getModule().getJsMajor()<9 ||
                    (getModule().getJsMajor()==9 && getModule().getJsMinor()<1))) {
            // previous versions did not set dynamic flag on members
            t.makeMembersDynamic();
        }
        return t;
    }

    /** Loads an object declaration, creating it if necessary, and returns its type declaration. */
    @SuppressWarnings("unchecked")
    TypeDeclaration loadObject(String name, Map<String, Object> m, Scope parent, List<TypeParameter> existing) {
        Value obj;
        if (m.get(KEY_METATYPE) instanceof Value) {
            obj = (Value)m.get(KEY_METATYPE);
        } else {
            obj = new Value();
            m.put(KEY_METATYPE, obj);
            obj.setName(name);
            obj.setContainer(parent);
            obj.setScope(parent);
            obj.setUnit(u2);
            com.redhat.ceylon.model.typechecker.model.Class type = new com.redhat.ceylon.model.typechecker.model.Class();
            type.setName(name);
            type.setAnonymous(true);
            type.setUnit(u2);
            type.setContainer(parent);
            type.setScope(parent);
            if (parent == this) {
                u2.addDeclaration(obj);
                u2.addDeclaration(type);
            }
            parent.addMember(obj);
            obj.setType(type.getType());
            setAnnotations(obj, (Integer)m.get(KEY_PACKED_ANNS), m.get(KEY_ANNOTATIONS));
            setAnnotations(obj.getTypeDeclaration(), (Integer)m.remove(KEY_PACKED_ANNS), m.remove(KEY_ANNOTATIONS));
            if (type.getExtendedType() == null) {
                if (m.containsKey("super")) {
                    type.setExtendedType(getTypeFromJson((Map<String,Object>)m.remove("super"),
                            parent instanceof Declaration ? (Declaration)parent : null, existing));
                } else {
                    type.setExtendedType(getTypeFromJson(idobj, parent instanceof Declaration ? (Declaration)parent : null, existing));
                }
            }
            if (m.containsKey(KEY_SATISFIES)) {
                List<Map<String,Object>> stypes = (List<Map<String,Object>>)m.remove(KEY_SATISFIES);
                type.setSatisfiedTypes(parseTypeList(stypes, existing));
            }
            if (m.containsKey(KEY_INTERFACES)) {
                for (Map.Entry<String,Map<String,Object>> inner : ((Map<String,Map<String,Object>>)m.remove(KEY_INTERFACES)).entrySet()) {
                    loadInterface(inner.getKey(), inner.getValue(), type, existing);
                }
            }
            if (m.containsKey(KEY_CLASSES)) {
                for (Map.Entry<String,Map<String,Object>> inner : ((Map<String,Map<String,Object>>)m.remove(KEY_CLASSES)).entrySet()) {
                    loadClass(inner.getKey(), inner.getValue(), type, existing);
                }
            }
            if (m.containsKey(KEY_OBJECTS)) {
                for (Map.Entry<String,Map<String,Object>> inner : ((Map<String,Map<String,Object>>)m.remove(KEY_OBJECTS)).entrySet()) {
                    loadObject(inner.getKey(), inner.getValue(), type, existing);
                }
            }
            addAttributesAndMethods(m, type, existing);
        }
        return obj.getTypeDeclaration();
    }

    /** Load a type alias, creating it if necessary.
     * @return The TypeAlias declaration. */
    @SuppressWarnings("unchecked")
    private TypeAlias loadTypeAlias(String name, Map<String, Object> m, Scope parent, List<TypeParameter> existing) {
        TypeAlias alias;
        if (m.get(KEY_METATYPE) instanceof TypeAlias) {
            //It's been loaded already
            alias = (TypeAlias)m.get(KEY_METATYPE);
            if (m.size() == 1) {
                return alias;
            }
        } else {
            Declaration maybe = parent.getDirectMember(name, null, false);
            if (maybe == null) {
                alias = new TypeAlias();
                alias.setContainer(parent);
                alias.setScope(parent);
                alias.setName(name);
                alias.setUnit(u2);
                setAnnotations(alias, (Integer)m.remove(KEY_PACKED_ANNS), m.remove(KEY_ANNOTATIONS));
                m.put(KEY_METATYPE, alias);
            } else if (maybe instanceof TypeAlias) {
                alias = (TypeAlias)maybe;
            } else {
                throw new IllegalStateException(maybe + " should be an TypeAlias");
            }
        }
        //Gather available type parameters
        List<Map<String,Object>> listOfMaps = (List<Map<String,Object>>)m.get(KEY_TYPE_PARAMS);
        final List<TypeParameter> tparms;
        if (listOfMaps != null && alias.getTypeParameters().size()<listOfMaps.size()) {
            tparms = parseTypeParameters(listOfMaps, alias, existing);
            alias.setTypeParameters(tparms);
        } else {
            tparms = alias.getTypeParameters();
        }
        final List<TypeParameter> allparms = JsonPackage.merge(tparms, existing);
        //All interfaces extend Object, except aliases
        if (alias.getExtendedType() == null) {
            alias.setExtendedType(getTypeFromJson((Map<String,Object>)m.get("$alias"),
                    parent instanceof Declaration ? (Declaration)parent : null, allparms));
        }
        if (m.containsKey(KEY_SELF_TYPE)) {
            for (TypeParameter _tp : tparms) {
                if (_tp.getName().equals(m.get(KEY_SELF_TYPE))) {
                    alias.setSelfType(_tp.getType());
                }
            }
        }
        if (m.containsKey("of")) {
            alias.setCaseTypes(parseTypeList((List<Map<String,Object>>)m.remove("of"), allparms));
        }
        if (m.containsKey(KEY_SATISFIES)) {
            List<Map<String,Object>> stypes = (List<Map<String,Object>>)m.remove(KEY_SATISFIES);
            alias.setSatisfiedTypes(parseTypeList(stypes, allparms));
        }
        m.clear();
        m.put(KEY_METATYPE, alias);
        if (parent == this) {
            u2.addDeclaration(alias);
        }
        parent.addMember(alias);
        return alias;
    }

    /** Looks up a type from model data, creating it if necessary. The returned type will have its
     * type parameters substituted if needed. */
    private Type getTypeFromJson(Map<String, Object> m, Declaration container, List<TypeParameter> typeParams) {
        TypeDeclaration td = null;
        if (m.get(KEY_METATYPE) instanceof TypeDeclaration) {
            td = (TypeDeclaration)m.get(KEY_METATYPE);
            if (td instanceof ClassOrInterface && td.getUnit().getPackage() instanceof JsonPackage) {
                ((JsonPackage)td.getUnit().getPackage()).load(td.getName(), typeParams);
            }
        }
        final String tname = (String)m.get(KEY_NAME);
        if ("$U".equals(tname)) {
            m.put(KEY_METATYPE, unknown);
            return unknown.getType();
        }
        if (td == null && m.containsKey("comp")) {
            @SuppressWarnings("unchecked")
            final List<Map<String,Object>> tmaps = (List<Map<String,Object>>)m.get(KEY_TYPES);
            final ArrayList<Type> types = new ArrayList<>(tmaps.size());
            if ("u".equals(m.get("comp"))) {
                UnionType ut = new UnionType(u2);
                for (Map<String, Object> tmap : tmaps) {
                    types.add(getTypeFromJson(tmap, container, typeParams));
                }
                ut.setCaseTypes(types);
                td = ut;
            } else if ("i".equals(m.get("comp"))) {
                IntersectionType it = new IntersectionType(u2);
                for (Map<String, Object> tmap : tmaps) {
                    types.add(getTypeFromJson(tmap, container, typeParams));
                }
                it.setSatisfiedTypes(types);
                td = it;
            } else {
                throw new IllegalArgumentException("Invalid composite type '" + m.get("comp") + "'");
            }
        } else if (td == null) {
            final String pname = (String)m.get(KEY_PACKAGE);
            if (pname == null) {
                //It's a ref to a type parameter
                final List<TypeParameter> containerTypeParameters;
                if (container instanceof Constructor) {
                    containerTypeParameters = ((Generic)container.getContainer()).getTypeParameters();
                } else if (container instanceof Generic) {
                    containerTypeParameters = ((Generic)container).getTypeParameters();
                } else {
                    containerTypeParameters = null;
                }
                if (containerTypeParameters != null) {
                    for (TypeParameter typeParam : containerTypeParameters) {
                        if (typeParam.getName().equals(tname)) {
                            td = typeParam;
                        }
                    }
                }
                if (td == null && typeParams != null) {
                    for (TypeParameter typeParam : typeParams) {
                        if (typeParam.getName().equals(tname)) {
                            td = typeParam;
                        }
                    }
                }
            } else {
                String mname = (String)m.get(KEY_MODULE);
                if ("$".equals(mname)) {
                    mname = LANGUAGE_MODULE_NAME;
                }
                com.redhat.ceylon.model.typechecker.model.Package rp;
                if ("$".equals(pname) || LANGUAGE_MODULE_NAME.equals(pname)) {
                    //Language module package
                    rp = isLanguagePackage()? this :
                        getModule()
                            .getLanguageModule()
                            .getDirectPackage(LANGUAGE_MODULE_NAME);
                } else if (mname == null) {
                    //local type
                    if (".".equals(pname)) {
                        rp = this;
                        if (container instanceof TypeDeclaration && tname.equals(container.getName())) {
                            td = (TypeDeclaration)container;
                        }
                    } else {
                        rp = getModule().getDirectPackage(pname);
                    }
                } else {
                    rp = getModule().getPackage(pname);
                }
                if (rp == null) {
                    throw new CompilerErrorException("Package not found: " + pname);
                }
                if (rp != this && rp instanceof JsonPackage && !((JsonPackage)rp).loaded) {
                    ((JsonPackage) rp).loadIfNecessary();
                }
                final boolean nested = tname.indexOf('.') > 0;
                final String level1 = nested ? tname.substring(0, tname.indexOf('.')) : tname;
                if (rp != null && !nested) {
                    Declaration d = rp.getDirectMember(tname, null, false);
                    if (d instanceof TypeDeclaration) {
                        td = (TypeDeclaration)d;
                        if (td.isTuple()) {
                            if (m.containsKey(KEY_TYPES)) {
                                @SuppressWarnings("unchecked")
                                List<Map<String,Object>> elemaps = (List<Map<String,Object>>)m.get(KEY_TYPES);
                                ArrayList<Type> elems = new ArrayList<>(elemaps.size());
                                for (Map<String,Object> elem : elemaps) {
                                    elems.add(getTypeFromJson(elem, container, typeParams));
                                }
                                Type tail = elems.get(elems.size()-1);
                                if ((tail.isSequence() || tail.isSequential())
                                        && !tail.isTuple() && !tail.isEmpty()) {
                                    elems.remove(elems.size()-1);
                                } else {
                                    tail = null;
                                }
                                return u2.getTupleType(elems, tail, -1);
                            } else if (m.containsKey("count")) {
                                @SuppressWarnings("unchecked")
                                Map<String,Object> elem = (Map<String,Object>)m.get(KEY_TYPE);
                                Type[] elems = new Type[(int)m.remove("count")];
                                Arrays.fill(elems, getTypeFromJson(elem, container, typeParams));
                                return u2.getTupleType(Arrays.asList(elems), null, -1);
                            }
                        }
                    } else if (d instanceof FunctionOrValue) {
                        td = ((FunctionOrValue)d).getTypeDeclaration();
                    }
                }
                if (td == null && rp instanceof JsonPackage) {
                    if (nested) {
                        td = ((JsonPackage)rp).loadNestedType(tname, typeParams);
                    } else {
                        td = (TypeDeclaration)((JsonPackage)rp).load(tname, typeParams);
                    }
                }
                //Then look in the top-level declarations
                if (nested && td == null) {
                    for (Declaration d : rp.getMembers()) {
                        if (d instanceof TypeDeclaration && level1.equals(d.getName())) {
                            td = (TypeDeclaration)d;
                        }
                    }
                    final String[] path = tname.split("\\.");
                    for (int i = 1; i < path.length; i++) {
                        td = (TypeDeclaration)td.getDirectMember(path[i], null, false);
                    }
                }
            }
        }
        //From 1.2.3 we stored type arguments in maps
        final Type newType = loadTypeArguments(m, td, container, typeParams);
        if (newType != null) {
            return newType;
        }
        //This is the old pre 1.2.3 stuff
        @SuppressWarnings("unchecked")
        final List<Map<String,Object>> modelParms = (List<Map<String,Object>>)m.get(KEY_TYPE_PARAMS);
        if (td != null && modelParms != null) {
            //Substitute type parameters
            final HashMap<TypeParameter, Type> concretes = new HashMap<>();
            HashMap<TypeParameter,SiteVariance> variances = null;
            if (td.getTypeParameters().size() < modelParms.size()) {
                if (td.getUnit().getPackage() == this) {
                    parseTypeParameters(modelParms, td, null);
                }
            }
            final Iterator<TypeParameter> viter = td.getTypeParameters().iterator();
            for (Map<String,Object> ptparm : modelParms) {
                TypeParameter _cparm = viter.next();
                if (ptparm.containsKey(KEY_PACKAGE) || ptparm.containsKey(KEY_TYPES)) {
                    //Substitute for proper type
                    final Type _pt = getTypeFromJson(ptparm, container, typeParams);
                    concretes.put(_cparm, _pt);
                } else if (ptparm.containsKey(KEY_NAME) && typeParams != null) {
                    //Look for type parameter with same name
                    for (TypeParameter typeParam : typeParams) {
                        if (typeParam.getName().equals(ptparm.get(KEY_NAME))) {
                            concretes.put(_cparm, typeParam.getType());
                        }
                    }
                }
                Integer usv = (Integer)ptparm.get(KEY_US_VARIANCE);
                if (usv != null) {
                    if (variances == null) {
                        variances = new HashMap<>();
                    }
                    variances.put(_cparm, SiteVariance.values()[usv]);
                }
            }
            if (!concretes.isEmpty()) {
                return td.getType().substitute(concretes, variances);
            }
        }
        if (td == null) {
            try {
                throw new IllegalArgumentException(String.format("Couldn't find type %s::%s for %s in %s<%s> (FROM pkg %s)",
                        m.get(KEY_PACKAGE), m.get(KEY_NAME),
                        m.get(KEY_MODULE), m, typeParams, getNameAsString()));
            } catch (IllegalArgumentException ex) {
                ex.printStackTrace();
            }
        }
        return td.getType();
    }

    /** Load the type arguments from a map where keys are the type argument names (including their types,
     * e.g. List.Element) and the keys are maps suitable for decoding with getTypeFromJson) */
    Type loadTypeArguments(Map<String, Object> m, final TypeDeclaration td,
            final Declaration container, final List<TypeParameter> typeParams) {
        if (td == null) {
            return null;
        }
        @SuppressWarnings("unchecked")
        final Map<String,Map<String,Object>> targs = (Map<String,Map<String,Object>>)m.get(
                KEY_TYPE_ARGS);
        if (targs == null) {
            return null;
        }
        //Substitute type parameters
        final HashMap<TypeParameter, Type> concretes = new HashMap<>(targs.size());
        HashMap<TypeParameter,SiteVariance> variances = null;
        Declaration d = td;
        while (d != null) {
            if (d instanceof Generic) {
                for (TypeParameter tparm : ((Generic)d).getTypeParameters()) {
                    Map<String,Object> targMap = targs.get(partiallyQualifiedName(d) + "." + tparm.getName());
                    if (targMap == null) {
                        //TODO error I guess
                        continue;
                    }
                    if (targMap.containsKey(KEY_PACKAGE) || targMap.containsKey(KEY_TYPES)) {
                        //Substitute for proper type
                        final Type _pt = getTypeFromJson(targMap, container, typeParams);
                        concretes.put(tparm, _pt);
                    } else if (targMap.containsKey(KEY_NAME) && typeParams != null) {
                        //Look for type parameter with same name
                        for (TypeParameter typeParam : typeParams) {
                            if (typeParam.getName().equals(targMap.get(KEY_NAME))) {
                                concretes.put(tparm, typeParam.getType());
                            }
                        }
                    }
                    Integer usv = (Integer)targMap.get(KEY_US_VARIANCE);
                    if (usv != null) {
                        if (variances == null) {
                            variances = new HashMap<>();
                        }
                        variances.put(tparm, SiteVariance.values()[usv]);
                    }
                }
            }
            d = ModelUtil.getContainingDeclaration(d);
        }
        if (!concretes.isEmpty()) {
            return td.getType().substitute(concretes, variances);
        }
        return null;
    }

    /** Load a top-level declaration with the specified name, by parsing its model data. */
    Declaration load(String name, List<TypeParameter> existing) {
        if (model == null) {
            throw new IllegalStateException("No model available to load " + getNameAsString() + "::" + name);
        }
        @SuppressWarnings("unchecked")
        final Map<String,Object> map = (Map<String,Object>)model.get(name);
        if (map == null) {
            if ("Nothing".equals(name)) {
                if (isLanguagePackage()) {
                    return nothing;
                } else if (getModule().getJsMajor()<9 ||
                        (getModule().getJsMajor()==9 && getModule().getJsMinor()<1)) {
                    //Older versions wrongly referenced a local Nothing, maintain backwards compat
                    return nothing;
                }
            } else if ("$U".equals(name)) {
                return unknown;
            }
            throw new IllegalStateException("Cannot find " + pkgname + "::" + name + " in " + model.keySet());
        }
        Object metatype = map.get(KEY_METATYPE);
        if (metatype == null) {
            throw new IllegalArgumentException("Missing metatype from entry " + map);
        }
        if (metatype.equals(METATYPE_ATTRIBUTE)
                || metatype.equals(METATYPE_GETTER)) {
            return loadAttribute(name, map, this, null);
        } else if (metatype.equals(METATYPE_CLASS) || metatype instanceof com.redhat.ceylon.model.typechecker.model.Class) {
            return loadClass(name, map, this, existing);
        } else if (metatype.equals(METATYPE_INTERFACE) || metatype instanceof com.redhat.ceylon.model.typechecker.model.Interface) {
            return loadInterface(name, map, this, existing);
        } else if (metatype.equals(METATYPE_METHOD)) {
            return loadMethod(name, map, this, existing);
        } else if (metatype.equals(METATYPE_OBJECT) || metatype instanceof Value) {
            return loadObject(name, map, this, existing);
        } else if (metatype.equals(METATYPE_ALIAS) || metatype instanceof TypeAlias) {
            return loadTypeAlias(name, map, this, existing);
        }
        System.out.println("WTF is this shit " + map);
        return null;
    }

    public static boolean hasAnnotationBit(int bits, String annotationName) {
        final int idx = annotationBits.indexOf(annotationName);
        if (idx < 0) return false;
        return (bits & (1 << idx)) > 0;
    }

    private void setBitAnnotations(Declaration d, int bits) {
        d.setShared(hasAnnotationBit(bits, "shared"));
        d.setActual(hasAnnotationBit(bits, "actual"));
        d.setFormal(hasAnnotationBit(bits, "formal"));
        d.setDefault(hasAnnotationBit(bits, "default"));
        d.setNativeBackends(hasAnnotationBit(bits, "native") ? Backend.JavaScript.asSet() : Backends.ANY);
        d.setAnnotation(hasAnnotationBit(bits, "annotation"));
        d.setStatic(hasAnnotationBit(bits, "static"));
        if (hasAnnotationBit(bits, "sealed")) {
            ((TypeDeclaration)d).setSealed(true);
        }
        if (d instanceof com.redhat.ceylon.model.typechecker.model.Class) {
            ((com.redhat.ceylon.model.typechecker.model.Class)d).setFinal(hasAnnotationBit(bits, "final"));
            ((com.redhat.ceylon.model.typechecker.model.Class)d).setAbstract(hasAnnotationBit(bits, "abstract"));
        } else if (d instanceof Constructor) {
            ((Constructor)d).setAbstract(hasAnnotationBit(bits, "abstract"));
        }
        if (hasAnnotationBit(bits, "late")) {
            ((Value)d).setLate(true);
        }
        if (hasAnnotationBit(bits, "variable")) {
            ((Value)d).setVariable(true);
        }
    }

    @SuppressWarnings("unchecked")
    private void setAnnotations(Declaration d, Integer bits, Object anns) {
        if (bits != null) {
            setBitAnnotations(d, bits);
        }
        if (anns == null) return;
        if (anns instanceof List) {
            setNewAnnotations(d.getAnnotations(), (List<Map<String,List<String>>>)anns);
        } else if (anns instanceof Map) {
            setOldAnnotations(d.getAnnotations(), (Map<String,List<String>>)anns);
        } else {
            throw new IllegalArgumentException("Annotations should be a List (new format) or a Map (old format)");
        }
        
        for (Annotation a: d.getAnnotations()) {
            if (a.getName().equals("restricted") && !a.getPositionalArguments().isEmpty()) {
                d.setRestrictions(a.getPositionalArguments());
            }
        }
    }
    
    static void setNewAnnotations(List<Annotation> existing, List<Map<String,List<String>>> anns) {
        for (Map<String, List<String>> a : anns) {
            String name = a.keySet().iterator().next();
            Annotation ann = new Annotation();
            ann.setName(name);
            for (String arg : a.get(name)) {
                ann.addPositionalArgument(arg);
            }
            existing.add(ann);
        }
    }

    static void setOldAnnotations(List<Annotation> existing, Map<String,List<String>> m) {
        for (Map.Entry<String, List<String>> e : m.entrySet()) {
            String name = e.getKey();
            Annotation ann = new Annotation();
            ann.setName(name);
            for (String arg : e.getValue()) {
                ann.addPositionalArgument(arg);
            }
            existing.add(ann);
        }
    }

    /** Load a nested type that hasn't been loaded yet */
    private TypeDeclaration loadNestedType(final String fqn, List<TypeParameter> typeParams) {
        try{
        String[] path = fqn.split("\\.");
        @SuppressWarnings("unchecked")
        Map<String,Object> typeMap = (Map<String,Object>)model.get(path[0]);
        if (typeMap.get(KEY_METATYPE) instanceof TypeDeclaration == false) {
            load(path[0], typeParams);
        }
        TypeDeclaration td = (TypeDeclaration)typeMap.get(KEY_METATYPE);
        for (int i = 1; i < path.length; i++) {
            @SuppressWarnings("unchecked")
            Map<String,Object> subtypes = (Map<String,Object>)typeMap.get(KEY_INTERFACES);
            Map<String,Object> childMap = null;
            int type = 0;
            if (subtypes != null) {
                childMap = (Map<String,Object>)subtypes.get(path[i]);
                type = 1;
            }
            if (childMap == null) {
                subtypes = (Map<String,Object>)typeMap.get(KEY_CLASSES);
                if (subtypes != null) {
                    childMap = (Map<String,Object>)subtypes.get(path[i]);
                    type = 2;
                }
            }
            Declaration member = td.getDirectMember(path[i], null, false);
            TypeDeclaration child;
            if(member instanceof Value
                    && ((Value) member).getTypeDeclaration() instanceof Constructor)
                child = ((Value) member).getTypeDeclaration().getExtendedType().getDeclaration();
            else
                child = (TypeDeclaration) member;
            
            if (child == null) {
                switch(type) {
                case 1:child = loadInterface(path[i], childMap, td, typeParams);
                break;
                case 2:child = loadClass(path[i], childMap, td, typeParams);
                break;
                }
            }
            td = child;
        }
        return td;
        }catch(RuntimeException x){
            throw new RuntimeException("Failed to load inner type "+fqn+" in package "+getQualifiedNameString(), x);
        }
    }

    /** Create a new list that contains all the type parameters in the first list,
     * and the type parameters from the second list that don't have any names already present in the first list.
     */
    public static List<TypeParameter> merge(List<TypeParameter> l1, List<TypeParameter> l2) {
        int size = (l1 == null ? 0 : l1.size()) + (l2 == null ? 0 : l2.size());
        ArrayList<TypeParameter> merged = new ArrayList<>(size);
        HashSet<String> names = new HashSet<>();
        if (l1 != null) {
            for (TypeParameter t : l1) {
                merged.add(t);
                names.add(t.getName());
            }
        }
        if (l2 != null) {
            for (TypeParameter t : l2) {
                if (!names.contains(t.getName())) {
                    merged.add(t);
                }
            }
        }
        return merged;
    }

}
