//    XSD Diagram - Kotlin/JavaFX port
//    ItemsChoiceType4.kt: nombres locales posibles dentro de xs:complexType
//    (equivale al enum XMLSchema.ItemsChoiceType4 de XMLSchema.cs).
//
//    Licencias del original: LGPL / MS-PL.
package xsddiagramx.parsing

/** Nombres locales posibles dentro de xs:complexType (equivale a ItemsChoiceType4). */
enum class ItemsChoiceType4 {
    group, all, choice, sequence,
    complexContent, simpleContent,
    attribute, attributeGroup, anyAttribute
}
