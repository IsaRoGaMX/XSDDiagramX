//    XSD Diagram - Kotlin/JavaFX port
//    Port de XSDDiagrams\DiagramHelpers.cs
//
//    Licencias del original: LGPL / MS-PL (véase LICENSE-LGPL.txt / LICENSE-MS-PL.txt)
//
//    Helpers de dominio: resolución de atributos (con herencia por extensión y
//    restricción) y extracción de documentación de las annotations.
package xsddiagramx

import xsddiagramx.parsing.Annotated
import xsddiagramx.parsing.AttrAttribute
import xsddiagramx.parsing.AttrAttributeGroup
import xsddiagramx.parsing.ComplexAnyAttribute
import xsddiagramx.parsing.ComplexAttribute
import xsddiagramx.parsing.ComplexAttributeGroup
import xsddiagramx.parsing.ComplexComplexContent
import xsddiagramx.parsing.ComplexSimpleContent
import xsddiagramx.parsing.ItemsChoiceType4
import xsddiagramx.parsing.QualifiedName
import xsddiagramx.parsing.Schema
import xsddiagramx.parsing.SimpleList
import xsddiagramx.parsing.SimpleRestriction
import xsddiagramx.parsing.XSDAttribute
import xsddiagramx.parsing.XSDObject
import xsddiagramx.parsing.annotation
import xsddiagramx.parsing.attribute
import xsddiagramx.parsing.attributeGroup
import xsddiagramx.parsing.complexType
import xsddiagramx.parsing.element
import xsddiagramx.parsing.extensionType
import xsddiagramx.parsing.restrictionType
import xsddiagramx.parsing.simpleType
import xsddiagramx.parsing.XSDAttributeGroup

/** Equivale a la clase estática DiagramHelpers del C#. */
object DiagramHelpers {

    /** Equivale a QualifiedNameToFullName(string type, XmlQualifiedName). */
    fun qualifiedNameToFullName(type: String, xmlQualifiedName: QualifiedName): String =
        "${xmlQualifiedName.namespace}:$type:${xmlQualifiedName.name}"

    /** Equivale a QualifiedNameToAttributeTypeName: "name  : namespace" para
     *  la columna "Type" de la tabla de atributos. */
    private fun qualifiedNameToAttributeTypeName(xmlQualifiedName: QualifiedName): String =
        "${xmlQualifiedName.name}  : ${xmlQualifiedName.namespace}"

    // ------------------------------------------------------------------
    // Atributos de un complexType (incluida la herencia)
    // ------------------------------------------------------------------

    /** Equivale a ParseComplexTypeAttributes: recorre los ítems del complexType
     *  (attribute/attributeGroup/anyAttribute) y las bases de extensión o
     *  restricción, acumulando los XSDAttribute propios y heredados. */
    private fun parseComplexTypeAttributes(
        schema: Schema,
        nameSpace: String,
        listAttributes: MutableList<XSDAttribute>,
        complexTypeElement: complexType,
        isRestriction: Boolean,
    ) {
        for (i in complexTypeElement.itemsElementName.indices) {
            when (complexTypeElement.itemsElementName[i]) {
                ItemsChoiceType4.attribute -> {
                    val attr = (complexTypeElement.items[i] as ComplexAttribute).value
                    parseAttribute(schema, nameSpace, listAttributes, attr, false)
                }
                ItemsChoiceType4.attributeGroup -> {
                    val attrGroup = (complexTypeElement.items[i] as ComplexAttributeGroup).value
                    parseAttributeGroup(schema, nameSpace, listAttributes, attrGroup, false)
                }
                ItemsChoiceType4.anyAttribute -> {
                    val wildcard = (complexTypeElement.items[i] as ComplexAnyAttribute).value
                    listAttributes.add(
                        XSDAttribute("", "*", wildcard.namespace ?: "", "", false, null, null, null)
                    )
                }
                ItemsChoiceType4.simpleContent, ItemsChoiceType4.complexContent -> {
                    val annotatedContent = when (val item = complexTypeElement.items[i]) {
                        is ComplexComplexContent -> item.value.item
                        is ComplexSimpleContent -> item.value.item
                        else -> null
                    }
                    if (annotatedContent is extensionType) {
                        val extensionType = annotatedContent
                        val extensionBase = extensionType.base
                        val xsdExtensionType: XSDObject? =
                            if (extensionBase != null) {
                                schema.elementsByName[qualifiedNameToFullName("type", extensionBase)]
                            } else null

                        if (xsdExtensionType != null) {
                            val annotatedExtension = xsdExtensionType.tag as? Annotated
                            if (annotatedExtension is complexType) {
                                parseComplexTypeAttributes(
                                    schema,
                                    extensionBase!!.namespace ?: "",
                                    listAttributes,
                                    annotatedExtension,
                                    false
                                )
                            }
                        }
                        for (annotated in extensionType.items) {
                            when (annotated) {
                                is AttrAttribute -> parseAttribute(schema, nameSpace, listAttributes, annotated.value, false)
                                is AttrAttributeGroup -> parseAttributeGroup(schema, nameSpace, listAttributes, annotated.value, false)
                                else -> {}
                            }
                        }
                    } else if (annotatedContent is restrictionType) {
                        val restrictionType = annotatedContent
                        val restrictionBase = restrictionType.base
                        val xsdRestrictionType: XSDObject? =
                            if (restrictionBase != null) {
                                schema.elementsByName[qualifiedNameToFullName("type", restrictionBase)]
                            } else null

                        if (xsdRestrictionType != null) {
                            val annotatedRestriction = xsdRestrictionType.tag as? Annotated
                            if (annotatedRestriction is complexType) {
                                parseComplexTypeAttributes(
                                    schema,
                                    restrictionBase!!.namespace ?: "",
                                    listAttributes,
                                    annotatedRestriction,
                                    false
                                )
                            }
                        }
                        for (annotated in restrictionType.items1) {
                            when (annotated) {
                                is AttrAttribute -> parseAttribute(schema, nameSpace, listAttributes, annotated.value, true)
                                is AttrAttributeGroup -> parseAttributeGroup(schema, nameSpace, listAttributes, annotated.value, true)
                                else -> {}
                            }
                        }
                    }
                }
                else -> {}
            }
        }
    }

    /** Equivale a ParseAttribute(...): devuelve el XSDAttribute insertado, o null. */
    private fun parseAttribute(
        schema: Schema,
        nameSpaceIn: String,
        listAttributes: MutableList<XSDAttribute>,
        attr: attribute,
        isRestriction: Boolean,
    ): XSDAttribute? {
        var nameSpace = nameSpaceIn
        var isReference = false
        val filename = ""
        var name: String? = attr.name
        var type = ""

        val ref = attr.ref
        if (ref != null) {
            val o = schema.attributesByName[qualifiedNameToFullName("attribute", ref)]
            if (o is XSDAttribute) {
                val refXSDAttribute =
                    parseAttribute(schema, nameSpace, listAttributes, o.tag!!, isRestriction)
                if (refXSDAttribute != null) {
                    // Override the "use" field (comentario del original)
                    refXSDAttribute.use = attr.use
                }
                return null
            }
            // Reference not found!
            type = qualifiedNameToAttributeTypeName(ref)
            name = ref.name
            nameSpace = ref.namespace ?: ""
            isReference = true
        } else if (attr.type != null) {
            type = qualifiedNameToAttributeTypeName(attr.type!!)
            nameSpace = attr.type!!.namespace ?: ""
        } else if (attr.simpleType != null) {
            val body = attr.simpleType!!.item
            if (body is SimpleRestriction) {
                val base = body.base
                if (base != null) {
                    type = qualifiedNameToAttributeTypeName(base)
                    nameSpace = base.namespace ?: ""
                }
            } else if (body is SimpleList) {
                val itemType = body.itemType
                if (itemType != null) {
                    type = qualifiedNameToAttributeTypeName(itemType)
                    nameSpace = itemType.namespace ?: ""
                }
            }
        }

        if (isRestriction) {
            // xs:restriction > xs:attribute use="prohibited" marca el heredado.
            if (attr.use == "prohibited") {
                for (xsdAttribute in listAttributes) {
                    if (xsdAttribute.name == name) {
                        // listAttributes.remove(xsdAttribute); (comentado como en el C#)
                        xsdAttribute.use = attr.use
                        break
                    }
                }
            }
        } else {
            val xsdAttribute = XSDAttribute(
                filename,
                name ?: "",
                nameSpace,
                type,
                isReference,
                attr.defaultValue,
                attr.use,
                attr
            )
            listAttributes.add(0, xsdAttribute)
            return xsdAttribute
        }
        return null
    }

    /** Equivale a ParseAttributeGroup: expande el attributeGroup referenciado
     *  (xs:attributeGroup ref="...") añadiendo sus atributos listAttributes. */
    private fun parseAttributeGroup(
        schema: Schema,
        nameSpace: String,
        listAttributes: MutableList<XSDAttribute>,
        attributeGroupElement: attributeGroup,
        isRestriction: Boolean,
    ) {
        val ref = attributeGroupElement.ref
        if (ref != null) {
            val o = schema.attributesByName[qualifiedNameToFullName("attributeGroup", ref)]
            if (o is XSDAttributeGroup) {
                val attributeGroupInstance = o.tag
                for (annotated in attributeGroupInstance?.items ?: emptyList()) {
                    when (annotated) {
                        is AttrAttribute -> parseAttribute(schema, nameSpace, listAttributes, annotated.value, isRestriction)
                        is AttrAttributeGroup -> parseAttributeGroup(schema, nameSpace, listAttributes, annotated.value, isRestriction)
                        else -> {}
                    }
                }
            }
        }
    }

    /** Equivale a GetComplexTypeAttributes (privado en C#). */
    private fun getComplexTypeAttributes(
        schema: Schema,
        complexTypeElement: complexType,
        nameSpace: String,
    ): List<XSDAttribute> {
        val listAttributes = ArrayList<XSDAttribute>()
        parseComplexTypeAttributes(schema, nameSpace, listAttributes, complexTypeElement, false)
        return listAttributes
    }

    /** Equivale a GetAnnotatedAttributes(...): atributos visibles de un componente. */
    fun getAnnotatedAttributes(
        schema: Schema,
        annotated: Annotated,
        nameSpace: String,
    ): List<XSDAttribute> {
        val listAttributes = ArrayList<XSDAttribute>()
        when (annotated) {
            is element -> {
                if (annotated.complexType != null) {
                    listAttributes.addAll(getComplexTypeAttributes(schema, annotated.complexType!!, nameSpace))
                } else if (annotated.type != null) {
                    val xsdObject = schema.elementsByName[qualifiedNameToFullName("type", annotated.type!!)]
                    val annotatedElement = xsdObject?.tag as? Annotated
                    if (annotatedElement is complexType) {
                        listAttributes.addAll(getComplexTypeAttributes(schema, annotatedElement, nameSpace))
                    }
                }
            }
            is complexType -> {
                listAttributes.addAll(getComplexTypeAttributes(schema, annotated, nameSpace))
            }
            is simpleType -> {
                // Equivale al caso simpleType del C#: crea un atributo "virtual"
                // para mostrar el tipo simple en el panel de atributos.
                val attr = attribute()
                val def = simpleType()
                def.item = annotated.item
                attr.simpleType = def
                val type = (def.item as? SimpleRestriction)?.base?.name ?: ""
                listAttributes.add(
                    XSDAttribute("filename", annotated.name ?: "", "namespace", type, false, "", "", attr)
                )
            }
            else -> {}
        }
        return listAttributes
    }

    // ------------------------------------------------------------------
    // Documentación
    // ------------------------------------------------------------------

    /** Equivale a GetAnnotationText(XMLSchema.annotation): documentación multilenguaje. */
    fun getAnnotationText(annotationElement: annotation): String {
        var textDocumentation = ""
        for (documentation in annotationElement.items) {
            // Texto (equivale a documentation.Any[0].Value); si no hay, la fuente.
            val text = documentation.text
                ?: documentation.source
                ?: ""
            if (textDocumentation.isNotEmpty()) textDocumentation += "\r\n"
            textDocumentation += text

            if (!documentation.lang.isNullOrEmpty() || !documentation.source.isNullOrEmpty()) {
                textDocumentation += " ("
                if (!documentation.lang.isNullOrEmpty()) textDocumentation += documentation.lang
                if (!documentation.lang.isNullOrEmpty() && !documentation.source.isNullOrEmpty()) {
                    textDocumentation += ", "
                }
                if (!documentation.source.isNullOrEmpty()) textDocumentation += documentation.source
                textDocumentation += ")"
            }
        }
        return textDocumentation
    }
}
