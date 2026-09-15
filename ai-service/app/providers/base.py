from abc import ABC, abstractmethod

from app.schemas import AnalyseRequest, AnalyseResponse


class AnalysisProvider(ABC):
    """The provider abstraction layer referenced by ADR-0001 and ADR-0011.

    Every provider (mock, or a real hosted model) implements this single
    method; nothing above this layer needs to know which one is active.
    """

    @abstractmethod
    async def analyse(self, request: AnalyseRequest) -> AnalyseResponse: ...
