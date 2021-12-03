#See https://aka.ms/containerfastmode to understand how Visual Studio uses this Dockerfile to build your images for faster debugging.

FROM pspace/aspnetcore_for_api AS base
WORKDIR /app
EXPOSE 55080
EXPOSE 55443

FROM mcr.microsoft.com/dotnet/sdk:5.0-buster-slim AS build
WORKDIR /src
COPY ["PortalSvr/PortalSvr.csproj", "PortalSvr/"]
RUN dotnet restore "PortalSvr/PortalSvr.csproj"
COPY . .
WORKDIR "/src/PortalSvr"
RUN dotnet build "PortalSvr.csproj" -c Release -o /app/build

FROM build AS publish
RUN dotnet publish "PortalSvr.csproj" -c Release -o /app/publish

FROM base AS final
WORKDIR /app
COPY --from=publish /app/publish .
COPY --from=publish /app/publish/supervisord.conf /etc/supervisor/conf.d/supervisord.conf
ENTRYPOINT ["/usr/bin/supervisord"]
#ENTRYPOINT ["dotnet", "PortalSvr.dll"]
