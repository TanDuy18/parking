package org.example.duanparking.common.remote;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.time.LocalDate;
import java.util.List;

import org.example.duanparking.common.dto.*;
import org.example.duanparking.common.dto.rent.RentEvent;
import org.example.duanparking.common.dto.rent.RentResult;
import org.example.duanparking.model.DisplayMode;

public interface ParkingInterface extends Remote {
   /*
   * Client gọi server sẽ trả lời
   */

   List<ParkingSlotDTO> getAllSlots() throws RemoteException;
   int updateSlotStatus(ParkingSlotDTO slot) throws RemoteException;
   void registerClient(ClientCallback client) throws RemoteException; // Đăng ký client
   void unregisterClient(ClientCallback client) throws RemoteException;
   void takeVehicleInFromSync(ParkingInEvent slot) throws RemoteException;
   void takeVehicleOutFromSync(ParkingOutEvent slot) throws RemoteException;
   boolean checkIdIn(String plateName) throws RemoteException;
   boolean checkPlaceIn (String placename) throws RemoteException;
   ParkingSlotDTO getVehicleInfoForOut(String plateNumber) throws RemoteException;
   ParkingSlotDTO getVehicleInfo(String plateNumber, DisplayMode mode) throws RemoteException;
   boolean takeVehicleOut(ParkingSlotDTO slot) throws RemoteException;
    RentEvent calculateRentPrice(RentEvent event) throws RemoteException;
    void getVehicleRentFromSync(RentEvent slot) throws RemoteException;
    RentResult acceptRentValue(RentEvent event) throws RemoteException;
    List<String> getRentalSpotOnDayWithSession(LocalDate date, String session) throws RemoteException;
    void ping() throws RemoteException;
    
}
