<?xml version='1.0' encoding='UTF-8'?>

<!-- $Id: r64-help.hs 7 2008-02-07 10:14:35Z binzm $ -->

<!DOCTYPE helpset
  PUBLIC "-//Sun Microsystems Inc.//DTD JavaHelp HelpSet Version 1.0//EN"
  "http://java.sun.com/products/javahelp/helpset_1_0.dtd">

<helpset version="1.0">

  <title>Route 64 Help</title>

  <maps>
   <homeID>top</homeID>
   <mapref location="r64-help-map.jhm"/>
  </maps>

  <view>
    <name>TOC</name>
    <label>TOC</label>
    <type>javax.help.TOCView</type>
    <data>r64-help-TOC.xml</data>
  </view>

  <view>
    <name>Search</name>
    <label>Search</label>
    <type>javax.help.SearchView</type>
    <data engine="com.sun.java.help.search.DefaultSearchEngine">
      JavaHelpSearch
    </data>
  </view>
</helpset>
