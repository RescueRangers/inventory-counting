package com.vestfiber.inventorycounting.DAL

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.DriverManager
import java.sql.SQLException

class CountingsService {

    suspend fun list() : Result<List<CountingData>> {
        var returnType: Result<List<CountingData>>

        withContext(Dispatchers.IO) {
            val jdbcUrl =
                "jdbc:jtds:sqlserver://172.25.194.30:1433;databaseName=InventoryCounting;user=Nuclear9714;password=HzqVXawq6sxYeKzXGNL"
            val conn = DriverManager.getConnection(jdbcUrl)
            conn.autoCommit = false
            val countings = mutableListOf<CountingData>()

            try {
                Class.forName("net.sourceforge.jtds.jdbc.Driver")
                val sql = "SELECT Id, CountingDate FROM InventoryCountings"
                val preparedStatement = conn.prepareStatement(sql)
                val result = preparedStatement.executeQuery()

                while (result.next()) {
                    val id = result.getInt("Id")
                    val countingDate = result.getDate("CountingDate")

                    countings.add(CountingData(id, countingDate))
                }
            } catch (ex: SQLException) {
                Log.e("Error : ", ex.message.toString())
                returnType = Result.Error(Exception("Błąd bazy danych"))
            } catch (ex1: ClassNotFoundException) {
                Log.e("Error : ", ex1.message.toString())
                returnType = Result.Error(Exception("Nie znaleziono klasy jtds.jdbc.Driver"))
            } catch (ex2: Exception) {
                Log.e("Error : ", ex2.message.toString())
                returnType = Result.Error(Exception(ex2.message))
            }
            returnType = Result.Success<List<CountingData>>(countings)
        }
        return returnType
    }
}