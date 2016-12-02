package com.intuit.wasabi.data.udf

class TextConcatFunc extends Function3[String, String, String, String] with Serializable {
  def apply(text1: String, delimiter: String, text2: String): String = new StringBuilder().append(text1).append(delimiter).append(text2).toString
}
