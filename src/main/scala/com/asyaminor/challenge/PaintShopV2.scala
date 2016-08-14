package com.asyaminor.challenge

import scala.io.Source

/**
  * Created by eunlu on 13/08/2016.
  */
object PaintShopV2 {

  type Bucket = List[ProducedPaint]
  type Likes = Int
  type Preference = (Int, Char)
  type Customer = List[Preference]
  type Choices = Map[String, List[Int]]

  private def getPaintType(colorType: Char): PaintType = colorType match {
    case 'M' => Matte()
    case 'G' => Gloss()
    case _ => throw new IllegalArgumentException
  }

  implicit class BucketPrint(bucket: Bucket) {
    def printBucket = bucket.foldLeft("")((str, paint) => str + " " + paint.typePaint.toString)
  }

  def fixThePaintBucket(bucket: Bucket, customerList: List[String]): Bucket = {
    def turnIntoTupleList(input: List[String]): Customer = {

      def turnInner(input: List[String], tuples: List[Preference]): List[(Int, Char)] = {
        input match {
          case i :: c :: rest =>
            val number = i.toInt
            val typeOfColor = c(0)
            turnInner(rest, (number, typeOfColor) :: tuples)
          case _ => tuples
        }
      }

      turnInner(input, List()).reverse
    }

    def increaseLikes(likes: List[Likes], index: Int): List[Likes] = {
      val current = likes(index)
      likes.updated(index, current + 1)
    }
    def decreaseLikes(likes: List[Likes], index: Int): List[Likes] = {
      val current = likes(index)

      if (current == 1) {
        throw new NoSolutionCanBeFound()
      }

      likes.updated(index, current - 1)
    }

    def customersOfPref(color: Int, paintType: PaintType, choices: Choices): Option[List[Int]] = {
      val key = color + paintType.toString
      //println(s"key: $key and choices: " + choices)
      choices.get(key)
    }

    def makePeopleHappy(color: Int, paintType: PaintType, choices: Choices, likes: List[Likes]): List[Likes] = {

      //println(s"making people happy with paint $paintType")

      customersOfPref(color + 1, paintType, choices) match {
        //customersOfPref(color, Matte(), choices) match {
        case Some(people) =>
          //println(s"found happy people: $people")
          people.foldLeft(likes)((likeS, person) => increaseLikes(likeS, person))
        case None => likes
      }
    }

    def makePeopleUnhappy(color: Int, paintType: PaintType, choices: Choices, likes: List[Likes]): List[Likes] = {

      //println(s"making people unhappy with paint $paintType")

      customersOfPref(color + 1, paintType, choices) match {
      //customersOfPref(color, Matte(), choices) match {
        case Some(people) =>
          people.foldLeft(likes)((likeS, person) => decreaseLikes(likeS, person))
        case None => likes
      }
    }

    def recordCustomerPref(choices: Choices, customerIndex: Int, paintType: PaintType, color: Int): Choices = {
      val key  = color + paintType.toString
      val pref = choices.get(key)

      pref match {
        case Some(preference) => choices.updated(key, customerIndex :: preference)
        case None => choices + (key -> List(customerIndex))
      }
    }

    def canWeMakeUnhappy(index: Likes, choices: Choices, likes: List[Likes]): Boolean = {
      // we are only turning to Gloss for getting cheaper

      //println(s"looking for index: ${index + 1} with Matte choices")

      customersOfPref(index + 1, Matte(), choices) match {
        case Some(people) =>
          //println(s"people for matte on index ${index + 1} count is: " + people.size)
          val filteredPeople = people.count{person =>
            //println(s"person number $person how many likes they have: ${likes(person)} with all likes ${likes}")
            likes(person) > 1}
          filteredPeople == people.size
        case None => true
      }
    }

    def makeItCheaper(bucket: Bucket,
                      choices: Choices, likes: List[Likes], index: Int): Bucket = {

      def makeCheapInner(bucket: Bucket, newBucket: Bucket,
                         choices: Choices, likes: List[Likes], index: Int): (Bucket, List[Likes]) = bucket match {
        case Nil => (newBucket, likes)
        case paint :: rest =>
          if (paint.typePaint == Matte() && canWeMakeUnhappy(index, choices, likes)) {
            //println("found Matte paint and we're making people unhappier a bit")
            val currentLikes = makePeopleUnhappy(index, Matte(), choices, likes)
            //println("unhappy people: " + currentLikes)
            val updatedLikes = makePeopleHappy(index, Gloss(), choices, currentLikes)
            //println("happier people: " + updatedLikes)
            makeCheapInner(rest, newBucket.updated(index, ProducedPaint(Gloss())), choices, updatedLikes, index + 1)
          }
          else {
            makeCheapInner(rest, newBucket, choices, likes, index + 1)
          }
      }

      val (updatedBucket, updatedLikes) = makeCheapInner(bucket, newBucket = bucket, choices, likes, index)

      //println("end of iteration: bucket: " + updatedBucket.printBucket)
      //println("end of iteration bucket earlier: " + bucket.printBucket)

      if (updatedBucket.equals(bucket)) {
        updatedBucket
      }
      else {
        makeItCheaper(updatedBucket, choices, updatedLikes, index)
      }

    }

    def solveForCustomer(cust: Customer, bucket: Bucket,
                         choices: Choices,
                         likes: List[Likes], index: Likes): (Bucket, Choices, List[Likes]) = {

      def getPaintFromBucket(bucket: Bucket, color: Int) = {
        bucket(color - 1).typePaint
      }

      val prefCount = cust.size
      //println(s"trying to solve for customer with index: $index, customer has $prefCount preferences left")
      //println("current likes: " + likes)

      cust match {
        case Nil => (bucket, choices, likes)
        case pref :: otherPrefs =>
          //if color matches
          val color = pref._1
          val paintType = getPaintType(pref._2)

        //  println(s"customer has pref for color: $color and paint type: $paintType")

          if (getPaintFromBucket(bucket, color) == paintType || paintType == Gloss()) {

            if (getPaintFromBucket(bucket, color) == paintType) {
              solveForCustomer(otherPrefs, bucket,
                recordCustomerPref(choices, index, paintType, color),
                increaseLikes(likes, index), index)
            }
            else {
              solveForCustomer(otherPrefs, bucket,
                recordCustomerPref(choices, index, paintType, color),
                likes, index)
            }

          }
          else {
          //  println(s"different type of colors for color: $color!!!")
//            println("bucket before switching paint types: " + bucket.printBucket)
            val bucketNew = bucket.updated(color - 1, ProducedPaint(paintType))

//            println("bucket after switching paint types: " + bucketNew.printBucket)
            val likesNew = makePeopleUnhappy(color - 1, getPaintFromBucket(bucket, color), choices, likes)
            solveForCustomer(otherPrefs, bucketNew , recordCustomerPref(choices, index, paintType, color),
              increaseLikes(likesNew, index), index)
          }
      }
    }

    val preferenceList = customerList map
      {customerString => turnIntoTupleList(customerString.split(" ").toList)}

    val sortedPrefs = preferenceList sortBy(pref => pref.size)
    //init likes list
    val likesByCustomers = List.fill(customerList.size)(0)
    //choices by customers
    val choices: Choices = Map()

    def tryToSolve(bucket: Bucket, choices: Choices, likes: List[Likes], customerList: List[Customer],
                   index: Int): Bucket = {
      customerList match {
        case Nil =>
          val bucketString = bucket.printBucket
//          println(s"before reduction: $bucketString")
          val revisedBucket = makeItCheaper(bucket, choices, likes, index = 0)
          //bucket //we can handle the reductions here!!! //TODO also check if there
          revisedBucket
        case cust :: rest =>
          val (bucketN, choicesN, likesN) = solveForCustomer(cust, bucket, choices, likes, index)
//          println("current bucket: " + bucketN.printBucket)
          tryToSolve(bucketN, choicesN, likesN, rest, index + 1)
      }
    }


    //foreach customer
    customerList match {
      case Nil => bucket
      case cust :: rest => tryToSolve(bucket, choices, likesByCustomers, sortedPrefs, 0)
    }
  }

  def main(args: Array[String]) {
    println("Hello PaintShop!")

    //TODO think about corner cases

    if (args.length == 0) {
      println("Filename must be supplied from the command line")
    }
    else {
      val fileName = args(0)

      val inputLines = Source.fromFile(fileName).getLines().toList

      require(inputLines.nonEmpty)

      val colorLength = inputLines(0).toInt
      val customerList = inputLines.tail

      val bucket = List.fill(colorLength)(ProducedPaint(Gloss()))

      try {
        val newBucket = fixThePaintBucket(bucket, customerList)

        println(newBucket.printBucket)
      }
      catch {
        case nsfe: NoSolutionCanBeFound => println("No solution exists")
        case t: Throwable => println("unknown error" + t.getMessage )
      }

    }
  }
}
