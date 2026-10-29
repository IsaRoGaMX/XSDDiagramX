//    XSD Diagram - Kotlin/JavaFX port
//    Port de XSDDiagrams\Parsing\XmlSchema.cs (código generado por xsd.exe, .NET)
//
//    Licencias del original: LGPL / MS-PL (véase LICENSE-LGPL.txt / LICENSE-MS-PL.txt)
//    Autores originales: Regis Cosnier (inicial), Paul Selormey (refactorización)
//
//    Este fichero define el MODELO DE DATOS que espeja el metamodelo de XML Schema
//    (http://www.w3.org/2001/XMLSchema). Mientras que el original se generaba con
//    la herramienta xsd.exe y se serializaba con XmlSerializer, aquí las clases se
//    escriben a mano y se rellenan mediante XmlSchemaReader (parser DOM del JDK).
//
//    Se portó el SUBCONJUNTO que la lógica de negocio (Schema, Diagram, DiagramItem,
//    DiagramItem, DiagramHelpers) usa realmente; las facetas de simpleType, key/keyref,
//    notation y redefine son placeholders.
//
//    Convención de nombres: se conservan los nombres originales en minúsculas
//    (schema, element, group...) para facilitar la comparación con el C# (los
//    nombres se respetaron tal como los generaba xsd.exe).
package xsddiagramx.parsing

/**
 * Equivale a System.Xml.XmlQualifiedName en el C#. El namespace es la URI resuelta
 * (no el prefijo); para nombres sin prefijo el namespace es la cadena vacía "",
 * igual que ocurría en el C# (los atributos sin prefijo no tienen namespace).
 */
data class QualifiedName(
    val namespace: String? = null,
    val name: String? = null,
) {
    override fun toString(): String =
        if (namespace.isNullOrEmpty()) (name ?: "") else "$namespace:$name"
}

/** Clase base de todos los nodos del esquema (equivale a XMLSchema.openAttrs). */
open class OpenAttrs

/** Equivale a XMLSchema.annotated: cualquier componente que puede llevar xs:annotation. */
open class Annotated : OpenAttrs() {
    var annotation: annotation? = null
}

// ---------------------------------------------------------------------------
// Raíz del documento: xs:schema
// ---------------------------------------------------------------------------

/** xs:schema (equivale a XMLSchema.schema). */
class schema : OpenAttrs() {
    var targetNamespace: String? = null
    var version: String? = null
    var finalDefault: String = ""
    var blockDefault: String = ""

    /** Primer grupo de hijos: annotation, import, include, redefine (document order). */
    val items: MutableList<OpenAttrs> = ArrayList()

    /** Segundo grupo de hijos: declaraciones globales (element, type, group, attribute...). */
    val items1: MutableList<OpenAttrs> = ArrayList()
}

/** xs:include. */
class include : Annotated() {
    var schemaLocation: String? = null
}

/** xs:import. (En Kotlin `import` es palabra reservada, por eso el sufijo). */
class schemaImport : Annotated() {
    var schemaLocation: String? = null
    var namespace: String? = null
}

/** xs:redefine. Placeholder: el original tampoco lo soporta (está en su TODO list). */
class redefine : Annotated()

/** xs:annotation. */
class annotation : OpenAttrs() {
    val items: MutableList<documentation> = ArrayList()
}

/**
 * xs:documentation. El contenido mixto (equivale a documentation.Any[0].Value en C#)
 * se reduce a la propiedad [text].
 */
class documentation : OpenAttrs() {
    var text: String? = null
    var lang: String? = null
    var source: String? = null
}

// ---------------------------------------------------------------------------
// Definiciones de tipo
// ---------------------------------------------------------------------------

/** xs:complexType (top-level y local: en el C# había topLevelComplexType y localComplexType). */
open class complexType : Annotated() {
    var name: String? = null
    var mixed: Boolean = false

    /**
     * Partículas y atributos del content model. Paralelo a [itemsElementName]
     * (mismo patrón Items / ItemsElementName que generaba xsd.exe para los
     * registros de xs:choice). Preserva el orden del documento.
     */
    val items: MutableList<ComplexItem> = ArrayList()
    val itemsElementName: MutableList<ItemsChoiceType4> = ArrayList()
}

/**
 * Contenido de xs:complexType. Los casos `group`/`all`/`choice`/`sequence`
 * envuelven un [group] (en C# todos mapeaban a XMLSchema.group con subclases).
 */
sealed class ComplexItem
class ComplexGroup(val value: group) : ComplexItem()
class ComplexComplexType(val value: complexType) : ComplexItem()
class ComplexComplexContent(val value: complexContent) : ComplexItem()
class ComplexSimpleContent(val value: simpleContent) : ComplexItem()
class ComplexAttribute(val value: attribute) : ComplexItem()
class ComplexAttributeGroup(val value: attributeGroup) : ComplexItem()
class ComplexAnyAttribute(val value: wildcard) : ComplexItem()

/** Nombres locales posibles dentro de xs:complexType (equivale a ItemsChoiceType4). */
enum class ItemsChoiceType4 {
    group, all, choice, sequence,
    complexContent, simpleContent,
    attribute, attributeGroup, anyAttribute
}

/** xs:facet (enumeration, pattern, length...). El original usaba XMLSchema.facet. */
class facet(var kind: String = "", var value: String? = null) : Annotated()

/** xs:simpleType (top-level, local dentro de xs:element/xs:attribute y anónimo). */
open class simpleType : Annotated() {
    var name: String? = null
    var item: SimpleTypeBody? = null
}

sealed class SimpleTypeBody

/** xs:restriction simple. Sólo se necesita el base + lista de facetas (sin procesar). */
class SimpleRestriction(val base: QualifiedName?) : SimpleTypeBody() {
    val facets: MutableList<facet> = ArrayList()
}

/** xs:list. */
class SimpleList(val itemType: QualifiedName?) : SimpleTypeBody()

/** xs:union. */
class SimpleUnion(val memberTypes: List<QualifiedName?>) : SimpleTypeBody()

/** xs:group / compositor (tnxs:all, xs:choice, xs:sequence y xs:group comparten clase). */
open class group : Annotated() {
    var name: String? = null

    /** Determinado sólo en referencias (xs:group ref="..."). */
    var ref: QualifiedName? = null
    var minOccurs: String = "1"
    var maxOccurs: String = "1"

    /** Contenido del compositor/o grupo; paralelo a [itemsElementName]. */
    val items: MutableList<GroupItem> = ArrayList()
    val itemsElementName: MutableList<ItemsChoiceType2> = ArrayList()
}

sealed class GroupItem
class GroupElement(val value: element) : GroupItem()
class GroupAny(val value: any) : GroupItem()
class GroupCompositor(val value: group) : GroupItem()

/** Nombres locales posibles dentro de xs:group / compositores (equivale a ItemsChoiceType2). */
enum class ItemsChoiceType2 { element, any, group, all, choice, sequence }

/** xs:complexContent. */
class complexContent : Annotated() {
    var item: ContentBody? = null
}

/** xs:simpleContent. */
class simpleContent : Annotated() {
    var item: ContentBody? = null
}

/** xs:extension / xs:restriction dentro de contenido simple o complejo. */
sealed class ContentBody : Annotated()

/** xs:complexContent > xs:extension (equivale a XMLSchema.extensionType). */
class extensionType(
    base: QualifiedName? = null,
) : ContentBody() {
    var base: QualifiedName? = base

    /** El original tenía propiedades separadas (group/sequence/choice/all): se conservan. */
    var group: group? = null
    var sequence: group? = null
    var choice: group? = null
    var all: group? = null

    /** xs:attribute | xs:attributeGroup directos. */
    val items: MutableList<AttrItem> = ArrayList()
}

/** xs:complexContent > xs:restriction (equivale a XMLSchema.restrictionType). */
class restrictionType(
    base: QualifiedName? = null,
) : ContentBody() {
    var base: QualifiedName? = base

    /** Partículas del restriction (usadas por el fix del "github issue 2"). */
    val items: MutableList<ComplexItem> = ArrayList()
    val itemsElementName: MutableList<ItemsChoiceType4> = ArrayList()

    /** xs:attribute | xs:attributeGroup del restriction (equivale a Items1). */
    val items1: MutableList<AttrItem> = ArrayList()
}

// ---------------------------------------------------------------------------
// Elementos de partícula
// ---------------------------------------------------------------------------

/**
 * xs:element. Una sola clase para top-level y local (en C#: topLevelElement,
 * localElement y narrowMaxMin).
 * OJO: `abstract` es palabra reservada en Kotlin, por eso el nombre [isAbstract].
 */
class element : Annotated() {
    var name: String? = null
    var type: QualifiedName? = null
    var ref: QualifiedName? = null
    var substitutionGroup: QualifiedName? = null
    var isAbstract: Boolean = false
    var nillable: Boolean = false
    var minOccurs: String = "1"
    var maxOccurs: String = "1"

    /** xs:element > xs:complexType inline (equivale a `element.Item`). */
    var complexType: complexType? = null

    /** xs:element > xs:simpleType inline. */
    var simpleType: simpleType? = null
}

/** xs:any (equivale a XMLSchema.any, que deriva de wildcard). */
class any : Annotated() {
    var namespace: String? = null
    var minOccurs: String = "1"
    var maxOccurs: String = "1"
}

/** xs:anyAttribute (equivale a XMLSchema.wildcard). */
class wildcard : Annotated() {
    var namespace: String? = null
}

// ---------------------------------------------------------------------------
// Atributos
// ---------------------------------------------------------------------------

/** xs:attribute (top-level y local). El `use` original era un enum; aquí es String tal cual. */
class attribute : Annotated() {
    var name: String? = null
    var type: QualifiedName? = null
    var ref: QualifiedName? = null
    var defaultValue: String? = null
    var fixed: String? = null
    /** "optional" (defecto) | "required" | "prohibited" (equivale a attributeUse.ToString()). */
    var use: String? = null

    /** xs:attribute > xs:simpleType inline (equivale a localSimpleType). */
    var simpleType: simpleType? = null
}

/** xs:attributeGroup (local y top-level; en C#: attributeGroupRef y namedAttributeGroup). */
open class attributeGroup : Annotated() {
    var name: String? = null
    var ref: QualifiedName? = null

    /** xs:attribute | xs:attributeGroup directos. */
    val items: MutableList<AttrItem> = ArrayList()
}

sealed class AttrItem
class AttrAttribute(val value: attribute) : AttrItem()
class AttrAttributeGroup(val value: attributeGroup) : AttrItem()
class AttrAnyAttribute(val value: wildcard) : AttrItem()
