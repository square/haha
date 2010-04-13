<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:template match="/leaks">
		<root>
			<xsl:apply-templates select="leak"/>
		</root>
	</xsl:template>
	
	<xsl:template match="leak">
		<xsl:apply-templates select="tree"/>
	</xsl:template>
	
	<xsl:template match="object">
		<xsl:element name="item">
			<xsl:attribute name="id">pxml_<xsl:value-of select="@id"/></xsl:attribute>
			<content><name><xsl:if test="outbound-ref"><xsl:value-of select="outbound-ref"/> in </xsl:if><xsl:value-of select="class"/></name></content>
			<xsl:apply-templates select="incoming-fields/incoming-field" />
		</xsl:element>
	</xsl:template>
	
	<xsl:template match="incoming-field">
		<xsl:apply-templates select="object"/>
	</xsl:template>
</xsl:stylesheet>