package com.vestfiber.inventorycounting.DAL
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.DriverManager
import java.sql.SQLException

class VfService {

    suspend fun listVfsOnScan(countingId: Int) : Result<List<VfBatchNumber>> {
        var returnType: Result<List<VfBatchNumber>>

        withContext(Dispatchers.IO) {
            val jdbcUrl =
                "jdbc:jtds:sqlserver://172.25.194.30:1433;databaseName=InventoryCounting;user=Nuclear9714;password=HzqVXawq6sxYeKzXGNL"
            val conn = DriverManager.getConnection(jdbcUrl)
            conn.autoCommit = false
            val loadedVfs = mutableListOf<VfBatchNumber>()

            try {
                Class.forName("net.sourceforge.jtds.jdbc.Driver")
                val sql = "SELECT [VfNumber]\n" +
                        "      ,[CountingId]\n" +
                        "  FROM [dbo].[VFs]\n" +
                        "  WHERE CountingId = ?"
                val preparedStatement = conn.prepareStatement(sql)

                preparedStatement.setInt(1, countingId)

                val result = preparedStatement.executeQuery()

                while (result.next()) {
                    val vfNumber = result.getString("VfNumber")

                    loadedVfs.add(VfBatchNumber(vfNumber, countingId, null))
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
            returnType = Result.Success<List<VfBatchNumber>>(loadedVfs)
        }
        return returnType
    }

    suspend fun saveScannedVfs(vfs: List<VfBatchNumber>) : Result<String>{
        var returnType: Result<String>

        withContext(Dispatchers.IO) {
            val jdbcUrl =
                "jdbc:jtds:sqlserver://172.25.194.30:1433;databaseName=InventoryCounting;user=Nuclear9714;password=HzqVXawq6sxYeKzXGNL"
            val conn = DriverManager.getConnection(jdbcUrl)
            conn.autoCommit = false

            try {
                Class.forName("net.sourceforge.jtds.jdbc.Driver")
                val sql = "BEGIN TRY\n" +
                        "   INSERT INTO [dbo].[ScannedVfs]\n" +
                        "           ([VfNumber]\n" +
                        "           ,[CountingId])\n" +
                        "     VALUES\n" +
                        "          (?, ?)\n" +
                        "END TRY\n" +
                        "BEGIN CATCH\n" +
                        "    IF ERROR_NUMBER() <> 2627\n" +
                        "      THROW\n" +
                        "END CATCH"
                val preparedStatement = conn.prepareStatement(sql)

                vfs.forEach {
                    preparedStatement.setString(1, it.vfNumber)
                    preparedStatement.setInt(2, it.countingId)
                    preparedStatement.addBatch()
                }
                preparedStatement.executeBatch()
                conn.commit()

                returnType = Result.Success<String>("Dodano numery do systemu")
            } catch (ex: SQLException) {
                if (conn != null) {
                    Log.i("Info : ", "Transaction is being rolled back")
                    conn.rollback()
                }
                Log.e("Error : ", ex.message.toString())
                returnType = Result.Error(Exception("Błąd bazy danych"))
            } catch (ex1: ClassNotFoundException) {
                Log.e("Error : ", ex1.message.toString())
                returnType = Result.Error(Exception("Nie znaleziono klasy jtds.jdbc.Driver"))
            } catch (ex2: Exception) {
                Log.e("Error : ", ex2.message.toString())
                returnType = Result.Error(Exception(ex2.message))
            }
        }
        return returnType
    }
}