//    XSD Diagram - Kotlin/JavaFX port
//    ComplexGroup.kt: subclase de [ComplexItem] para xs:group / xs:all / xs:choice /
//    xs:sequence dentro de xs:complexType (equivale a los casos group/all/choice/
//    sequence de XMLSchema.cs).
//
//    Licencias del original: LGPL / MS-PL.
package xsddiagramx.parsing

/**
 * Caso `group`, `all`, `choice` o `sequence` dentro de un xs:complexType: envuelve
 * el compositor/grupo correspondiente (todos comparten la clase [group], como en C#).
 */
class ComplexGroup(
    /** El compositor o la referencia xs:group (siempre se parsea como instancia de [group]). */
    val value: group,
) : ComplexItem()
