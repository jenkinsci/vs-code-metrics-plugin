<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:output method="xml" indent="no" />
  <xsl:template match="/">
    <report>
      <xsl:for-each select="CodeMetricsReport/Targets/Target/Modules/Module | CodeMetricsReport/Targets/Target/Assembly">
        <xsl:variable name="fullName"                   select="@Name"/>
        <xsl:variable name="moduleName"                 select="substring-before(concat($fullName,','),',')"/>
        <xsl:variable name="moduleMaintainabilityIndex" select="translate(Metrics/Metric[@Name = 'MaintainabilityIndex']/@Value, ',', '')" />
        <xsl:variable name="moduleCyclomaticComplexity" select="translate(Metrics/Metric[@Name = 'CyclomaticComplexity']/@Value, ',', '')" />
        <xsl:variable name="moduleClassCoupling"        select="translate(Metrics/Metric[@Name = 'ClassCoupling']/@Value, ',', '')" />
        <xsl:variable name="moduleDepthOfInheritance"   select="translate(Metrics/Metric[@Name = 'DepthOfInheritance']/@Value, ',', '')" />
        <xsl:variable name="moduleLinesOfCode"          select="translate(Metrics/Metric[@Name = 'LinesOfCode' or @Name = 'ExecutableLines']/@Value, ',', '')" />
        <module name="{$moduleName}" mi="{$moduleMaintainabilityIndex}" cyc="{$moduleCyclomaticComplexity}" cls="{$moduleClassCoupling}" doi="{$moduleDepthOfInheritance}" loc="{$moduleLinesOfCode}">
          <xsl:for-each select="./Namespaces/Namespace">
            <xsl:variable name="namespaceName"                 select="@Name"/>
            <xsl:variable name="namespaceMaintainabilityIndex" select="translate(Metrics/Metric[@Name = 'MaintainabilityIndex']/@Value, ',', '')" />
            <xsl:variable name="namespaceCyclomaticComplexity" select="translate(Metrics/Metric[@Name = 'CyclomaticComplexity']/@Value, ',', '')" />
            <xsl:variable name="namespaceClassCoupling"        select="translate(Metrics/Metric[@Name = 'ClassCoupling']/@Value, ',', '')" />
            <xsl:variable name="namespaceDepthOfInheritance"   select="translate(Metrics/Metric[@Name = 'DepthOfInheritance']/@Value, ',', '')" />
            <xsl:variable name="namespaceLinesOfCode"          select="translate(Metrics/Metric[@Name = 'LinesOfCode' or @Name = 'ExecutableLines']/@Value, ',', '')" />
            <namespace name="{$namespaceName}" mi="{$namespaceMaintainabilityIndex}" cyc="{$namespaceCyclomaticComplexity}" cls="{$namespaceClassCoupling}" doi="{$namespaceDepthOfInheritance}" loc="{$namespaceLinesOfCode}">
              <xsl:for-each select="./Types/Type">
                <xsl:variable name="typeName"                 select="@Name"/>
                <xsl:variable name="typeMaintainabilityIndex" select="translate(Metrics/Metric[@Name = 'MaintainabilityIndex']/@Value, ',', '')" />
                <xsl:variable name="typeCyclomaticComplexity" select="translate(Metrics/Metric[@Name = 'CyclomaticComplexity']/@Value, ',', '')" />
                <xsl:variable name="typeClassCoupling"        select="translate(Metrics/Metric[@Name = 'ClassCoupling']/@Value, ',', '')" />
                <xsl:variable name="typeDepthOfInheritance"   select="translate(Metrics/Metric[@Name = 'DepthOfInheritance']/@Value, ',', '')" />
                <xsl:variable name="typeLinesOfCode"          select="translate(Metrics/Metric[@Name = 'LinesOfCode' or @Name = 'ExecutableLines']/@Value, ',', '')" />
                <type name="{$typeName}" mi="{$typeMaintainabilityIndex}" cyc="{$typeCyclomaticComplexity}" cls="{$typeClassCoupling}" doi="{$typeDepthOfInheritance}" loc="{$typeLinesOfCode}">
                  <xsl:for-each select="./Members/Member">
                    <xsl:variable name="memberName"                 select="@Name"/>
                    <xsl:variable name="memberMaintainabilityIndex" select="translate(Metrics/Metric[@Name = 'MaintainabilityIndex']/@Value, ',', '')" />
                    <xsl:variable name="memberCyclomaticComplexity" select="translate(Metrics/Metric[@Name = 'CyclomaticComplexity']/@Value, ',', '')" />
                    <xsl:variable name="memberClassCoupling"        select="translate(Metrics/Metric[@Name = 'ClassCoupling']/@Value, ',', '')" />
                    <xsl:variable name="memberLinesOfCode"          select="translate(Metrics/Metric[@Name = 'LinesOfCode' or @Name = 'ExecutableLines']/@Value, ',', '')" />
                    <member name="{$memberName}" mi="{$memberMaintainabilityIndex}" cyc="{$memberCyclomaticComplexity}" cls="{$memberClassCoupling}" loc="{$memberLinesOfCode}" />
                  </xsl:for-each>
                </type>
              </xsl:for-each>
            </namespace>
          </xsl:for-each>
        </module>
      </xsl:for-each>
    </report>
  </xsl:template>
</xsl:stylesheet>
