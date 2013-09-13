import ceylon.language.model.declaration { VariableDeclaration }

shared interface Variable<Type> 
        satisfies Value<Type> {
    
    shared formal actual VariableDeclaration declaration;

    shared formal void set(Type newValue);
}
