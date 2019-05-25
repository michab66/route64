<?xml version='1.0' encoding='UTF-8'?>

<!-- 
  - Transforms the table of contents into an html representation.
  - &#x261c;
  - $Id: book.xsl 11 2008-09-20 11:06:39Z binzm $
  -->

<xsl:transform
  xmlns:xsl = "http://www.w3.org/1999/XSL/Transform"
  version = "1.0"
  xmlns:date="http://exslt.org/dates-and-times"
  extension-element-prefixes="date">

<xsl:output 
  method = "html" 
  doctype-public = "-//W3C//DTD XHTML 1.0 Strict//EN"
  indent = "no"/>



<!-- The table of contents structure that is used to generate the navigation
     bars and is loaded from an additional docuement when processing web
     pages.
     TODO: Currently the relative path to the actual toc document is defined
     in the build script.  It would be much cooler if we could say something
     like "use the toc file that is in the same directory like the source
     document". -->
<xsl:param
    name = "book.home"
    select = "."/>
<xsl:variable 
    name = "toc"
    select = "document($book.home)/toc" />



<!-- Matches the Table of contents and transforms this into an HTML structure
     linking to the book's chapters. -->
<xsl:template match="toc">
  <html>
    <head>
      <title><xsl:value-of select="./@title"/></title>
    </head>
    <body>
      <h1><xsl:value-of select="./@title"/></h1>
      <div><a href="../index.html">|| close ||</a></div>
      <hr/>
      <table border="0" width="100%">
      <colgroup><col width="20%"/><col width="80%"/></colgroup>
      <thead><tr align="left" valign="top">
      <td><xsl:for-each select="tocitem">
      <xsl:call-template name="tocitemHdl"/>
          </xsl:for-each></td>
      <td><xsl:copy-of select="frontpage"/></td>
      </tr></thead></table>
      <hr/>
      <p><a href="http://sourceforge.net">
        <img src="http://sourceforge.net/sflogo.php?group_id=19552&amp;type=1" 
                          width="88" 
                          height="31" 
                          border="0" 
                          alt="SourceForge.net Logo"/></a></p>
    </body>
  </html>
</xsl:template>



<!-- Matches a single table of contents entry.  -->
<xsl:template match="tocitem">
  <p>
    <xsl:element name="a">
      <xsl:attribute name="href">
        <xsl:value-of select="./@target"/>
      </xsl:attribute>
      <xsl:value-of select="./@text"/>
    </xsl:element>
  </p>
</xsl:template>
<xsl:template name="tocitemHdl">
  <p>
    <xsl:element name="a">
      <xsl:attribute name="href">
        <xsl:value-of select="./@target"/>
      </xsl:attribute>
      <xsl:value-of select="./@text"/>
    </xsl:element>
  </p>
</xsl:template>



<!-- Matches and transforms a chapter -->
<xsl:template match="chapter">

  <xsl:variable 
    name="id" 
    select="./@id" />
  <xsl:variable 
    name="currTocItem" 
    select="$toc/tocitem[@id=$id]" />
  <xsl:variable 
    name="prevTocItem" 
    select="$currTocItem/preceding-sibling::tocitem[1]" />
  <xsl:variable
    name="nextTocItem" 
    select="$currTocItem/following-sibling::tocitem[1]" />
  <xsl:variable
    name="title"
    select="$currTocItem/@text" />
  <xsl:variable
    name="booktitle"
    select="$toc/@title" />

  <html>
    <head>
      <title><xsl:value-of select="$title"/></title>
    </head>

    <body>
      <div>
      <!-- Start of header. -->
      <xsl:value-of select="$booktitle"/><br/>
      <!-- Handle the link to the previous chapter. -->
        <xsl:choose>
          <xsl:when test = "$prevTocItem">
            <!-- If we have a previous chapter then we beam in the link here. -->
            <xsl:element name="a">
              <xsl:attribute name="href">
                <xsl:value-of select="$prevTocItem/@target"/>
              </xsl:attribute>
              &lt;&lt;
            </xsl:element>
          </xsl:when>
          <xsl:otherwise>&lt;&lt;</xsl:otherwise>
        </xsl:choose>
      ||
      <!-- Handle the link to the table of contents. -->
        <xsl:element name="a">
          <xsl:attribute name="href">
            <xsl:value-of select="$toc/@target"/>
          </xsl:attribute>toc</xsl:element>
      ||
      <!-- Handle the link to the next chapter. -->
        <xsl:choose>
          <xsl:when test = "$nextTocItem">
            <!-- If we have a next chapter then we beam in the link here. -->
            <xsl:element name="a">
              <xsl:attribute name="href">
                <xsl:value-of select="$nextTocItem/@target"/>
              </xsl:attribute>
              &gt;<xsl:value-of select="$nextTocItem"/>&gt;
            </xsl:element>
          </xsl:when>
          <xsl:otherwise>&gt;&gt;</xsl:otherwise>
        </xsl:choose>
      </div>
      <!-- End of header. -->
      <hr/>
      <!-- Start of body. -->
      <h1><xsl:value-of select="$title"/></h1>
      <!-- Copy the chapter's contents to the target document. -->
      <xsl:copy-of select="*"/>
      <!-- End of body. -->
      <hr/>
      <!-- Start of footer. -->
      <table>
      <colgroup width="25%" span="4" />
      <tr>
      <td>Last modified:
          <xsl:value-of select="substring-before( substring-after( ./@modified, '$Date: ' ), ' ' )"/></td>
      <td>
        <a href="mailto:michab66@users.sourceforge.net">Comments</a></td>
      <!-- Dynamically compute the year for the copyright. -->
      <td>Copyright &#169; 
          <xsl:value-of select="date:year( date:date() )"/> 
          Michael G. Binz</td>
      <td><a href="http://sourceforge.net">
        <img src="http://sourceforge.net/sflogo.php?group_id=19552&amp;type=1" 
                          width="88" 
                          height="31"
                          border="0" 
                          alt="SourceForge.net Logo"/></a></td>
      </tr></table>
      <!-- End of footer. -->
    </body>
  </html>
</xsl:template>
</xsl:transform>
