//    XSD Diagram - Kotlin/JavaFX port
//    complexType.kt: xs:complexType del modelo del esquema (equivale a los tipos
//    topLevelComplexType / localComplexType de XMLSchema.cs, generado por xsd.exe).
//
//    Licencias del original: LGPL / MS-PL.
package xsddiagramx.parsing

/** xs:complexType (top-level y local: en el C# había topLevelComplexType y localComplexType). */
open class complexType : Annotated() {
    /** Nombre del tipo (o null si es tipo local anónimo). */
    var name: String? = null
    /** Atributo mixed del content model (permite texto en el elemento). */
    var mixed: Boolean = false

    /**
     * Partículas y atributos del content model. Paralelo a [itemsElementName]
     * (mismo patrón Items / ItemsElementName que generaba xsd.exe para los
     * registros de xs:choice). Preserva el orden del documento.
     */
    val items: MutableList<ComplexItem> = ArrayList()
    /** Etiqueta (valores de [ItemsChoiceType4]) en document order, paralela a [items]. */
    val itemsElementName: MutableList<ItemsChoiceType4> = ArrayList()
}
